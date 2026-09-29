package com.aktimetrix.it.orders;

import com.aktimetrix.core.api.Conformance;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import com.aktimetrix.it.ParcelMonitor;
import com.aktimetrix.it.support.EventSchemas;
import com.aktimetrix.it.support.TestBroker;
import com.aktimetrix.it.support.TestMonitor;
import com.aktimetrix.it.support.TestStore;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.aktimetrix.it.support.TestMonitor.await;
import static com.aktimetrix.it.support.TestMonitor.awaitTrue;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The order delivery use case of the white paper (section 1.1), end to end. Each subclass runs it on one message
 * broker and one state store.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class OrderDeliveryScenario {

    private static final String TOPIC = "shop-events";
    private TestBroker broker;
    private TestStore store;
    private TestMonitor monitor;

    protected abstract TestBroker broker(String eventsTopic);

    protected abstract TestStore store();

    @BeforeAll
    void start() {
        broker = broker(TOPIC);
        store = store();
        monitor = new TestMonitor(ParcelMonitor.class, "SHOP", broker, store, "aktimetrix.events.topic=" + TOPIC);
    }

    @AfterAll
    void stop() {
        monitor.close();
        store.close();
        broker.close();
    }

    /**
     * A priority customer's order, created at 09:00. Rules plan it: the order within 1 day, the delivery step within
     * 4 hours. A location update reports the distance half-way through the journey, and the order's fuel per km is
     * computed from its steps' measurements when it completes. Time is compared on every step; distance and fuel
     * while travelling, the parcel's temperature on delivery, and the customer's rating the next morning, after the
     * order completed, are compared with their plans.
     */
    @Test
    void comparesPlanAndActualInEveryDimension() {
        send("1234", "ORDER_CREATED", "2024-03-01 09:00:00", "{\"priority\":true,\"createdAt\":\"2024-03-01 09:00:00\"}");
        awaitTrue(() -> monitor.steps("1234").size() == 7, "the order's seven steps being created");
        send("1234", "ORDER_CONFIRMED", "2024-03-01 09:02:00", null);
        send("1234", "PAYMENT_CONFIRMED", "2024-03-01 09:20:00", null);
        send("1234", "HANDED_TO_AGENT", "2024-03-01 11:40:00", null);
        send("1234", "AGENT_ACCEPTED", "2024-03-01 11:50:00", null);
        send("1234", "TRAVEL_STARTED", "2024-03-01 11:55:00", null);
        send("1234", "LOCATION_UPDATED", "2024-03-01 12:20:00", "{\"route\":{\"distanceKm\":8}}");
        send("1234", "ARRIVED", "2024-03-01 12:45:00", "{\"route\":{\"distanceKm\":12},\"fuelLitres\":1.0}");
        send("1234", "DELIVERED", "2024-03-01 12:55:00", "{\"parcelTemperatureC\":40,\"deliveryCost\":9.5}");

        process("1234", p -> "Completed".equals(p.getStatus()));
        send("1234", "RATED", "2024-03-02 08:00:00", "{\"review\":{\"stars\":4}}");
        step("RATED", s -> "Completed".equals(s.getStatus()));
        ProcessInstance order = monitor.process("ORDER_DELIVERY", "1234").orElseThrow();
        assertThat(order.getPlannedAt()).isEqualTo(LocalDateTime.of(2024, 3, 2, 9, 0));
        assertThat(order.getTimeliness()).isEqualTo(Timeliness.ON_TIME);

        // the planning rule: priority customers are delivered within 4 hours
        assertThat(step("DELIVERED", s -> true).getPlannedAt()).isEqualTo(LocalDateTime.of(2024, 3, 1, 13, 0));
        assertThat(step("CONFIRM", s -> true).getTimeliness()).isEqualTo(Timeliness.ON_TIME);
        assertThat(step("PAY", s -> true).getTimeliness()).isEqualTo(Timeliness.ON_TIME);
        assertThat(step("HANDOVER", s -> true).getTimeliness()).isEqualTo(Timeliness.LATE);
        assertThat(step("TRAVEL", s -> true).getTimeliness()).isEqualTo(Timeliness.LATE);
        assertThat(step("DELIVERED", s -> true).getTimeliness()).isEqualTo(Timeliness.ON_TIME);

        assertActual("TRAVEL", "TIME", "PT25M", Conformance.OUT_OF_TOLERANCE);
        assertActual("TRAVEL", "DISTANCE", "7", Conformance.OUT_OF_TOLERANCE);
        assertActual("TRAVEL", "FUEL", "0.6", Conformance.OUT_OF_TOLERANCE);
        assertActual("DELIVERED", "TEMPERATURE", "10", Conformance.OUT_OF_TOLERANCE);
        assertActual("DELIVERED", "TIME", "PT-5M", Conformance.WITHIN_TOLERANCE);
        // the order as a whole: its cost, planned when it was created, actual on the event that completed it
        MeasurementInstance cost = measurement(m -> "COST".equals(m.getCode()) && "A".equals(m.getType()));
        assertThat(cost.getStepInstanceId()).as("process level").isNull();
        assertThat(cost.getDeviation()).isEqualTo("1.5");
        assertThat(cost.getConformance()).isEqualTo(Conformance.OUT_OF_TOLERANCE);
        // half-way through the journey, a location update already showed the route over plan
        MeasurementInstance reading = measurement(MeasurementInstance::isInterim);
        assertThat(reading.getCode()).isEqualTo("DISTANCE");
        assertThat(reading.getValue()).isEqualTo("8");
        assertThat(reading.getDeviation()).isEqualTo("3");
        assertThat(reading.getConformance()).isEqualTo(Conformance.OUT_OF_TOLERANCE);
        // the order's fuel per km, computed from the step measurements: 1.0 L / 12 km against 0.4 L / 5 km
        MeasurementInstance fuelPerKm = measurement(m -> "FUEL_PER_KM".equals(m.getCode()));
        assertThat(fuelPerKm.getDerivedFrom()).isEqualTo("FUEL / DISTANCE");
        assertThat(new BigDecimal(fuelPerKm.getValue())).isEqualByComparingTo("0.08333333333333333");
        assertThat(fuelPerKm.getPlannedValue()).isEqualTo("0.08");
        assertThat(fuelPerKm.getConformance()).isEqualTo(Conformance.WITHIN_TOLERANCE);
        // rated the next morning, after the order completed: one star below plan, within tolerance
        assertActual("RATED", "RATING", "-1", Conformance.WITHIN_TOLERANCE);

        assertPublishedEventsAreStructured();
    }

    /**
     * Every published event is valid against its schema, and tells which order and process it belongs to, and which
     * business event caused it.
     */
    private void assertPublishedEventsAreStructured() {
        final List<JsonNode> events = new ArrayList<>();
        // wait for the last event of each channel: the order completed, then rated
        for (String[] channel : new String[][]{{"process-instance-out-0", "\"eventCode\":\"COMPLETED\"", ""},
                {"step-instance-out-0", "\"eventCode\":\"COMPLETED\"", "\"stepCode\":\"RATED\""},
                {"measurement-instance-out-0", "\"eventCode\":\"RECORDED\"", "\"code\":\"RATING\""}}) {
            broker.received(channel[0], messages -> messages.stream().anyMatch(m -> m.contains(channel[1])
                            && m.contains(channel[2]) && m.contains("\"entityId\":\"1234\"")))
                    .forEach(message -> events.add(EventSchemas.assertValid(message)));
        }
        final List<JsonNode> order = new ArrayList<>();
        events.forEach(e -> {
            if ("1234".equals(e.at("/eventDetails/businessEntity/entityId").asText())) {
                order.add(e);
            }
        });
        assertThat(order).extracting(e -> e.path("eventType").asText())
                .contains("Process_Event", "Step_Event", "Measurement_Event");
        order.forEach(e -> {
            assertThat(e.at("/eventDetails/processCode").asText()).isEqualTo("ORDER_DELIVERY");
            assertThat(e.at("/eventDetails/definitionRevision").isIntegralNumber()).isTrue();
        });
        // the TRAVEL step completed on ARRIVED, at 12:45 in the business
        final JsonNode travelled = order.stream()
                .filter(e -> "Step_Event".equals(e.path("eventType").asText()) && "COMPLETED".equals(e.path("eventCode").asText())
                        && "TRAVEL".equals(e.at("/entity/stepCode").asText()))
                .findFirst().orElseThrow();
        assertThat(travelled.at("/eventDetails/cause/eventCode").asText()).isEqualTo("ARRIVED");
        assertThat(travelled.at("/eventDetails/occurredAt").asText()).isEqualTo("2024-03-01T12:45:00");
        // the order's measurement events say what kind they are
        assertThat(order.stream().filter(e -> "Measurement_Event".equals(e.path("eventType").asText()))
                .map(e -> e.path("eventCode").asText())).contains("PLANNED", "RECORDED", "READING", "METRIC");
    }

    /**
     * The definition changes while an order is running: the order started before the change keeps the definition it
     * started with, and one started after follows the new revision.
     */
    @Test
    void aRunningOrderKeepsTheDefinitionItStartedWith() {
        final ProcessDefinitionService definitions = monitor.bean(ProcessDefinitionService.class);
        send("5678", "ORDER_CREATED", "2024-03-01 09:00:00", "{\"priority\":true,\"createdAt\":\"2024-03-01 09:00:00\"}");
        final long before = process("5678", p -> true).getDefinitionRevision();

        // from now on, an order is cancelled by ORDER_WITHDRAWN rather than ORDER_CANCELLED
        final ProcessDefinition changed = definitions.findByCode("SHOP", "ORDER_DELIVERY");
        changed.setCancelEventCodes(List.of("ORDER_WITHDRAWN"));
        definitions.add(changed);
        send("9012", "ORDER_CREATED", "2024-03-01 10:00:00", "{\"priority\":true,\"createdAt\":\"2024-03-01 10:00:00\"}");
        assertThat(process("9012", p -> true).getDefinitionRevision()).isEqualTo(before + 1);

        send("9012", "ORDER_WITHDRAWN", "2024-03-01 10:05:00", null);
        send("5678", "ORDER_WITHDRAWN", "2024-03-01 10:05:00", null);
        process("9012", p -> "Cancelled".equals(p.getStatus()));
        assertThat(process("5678", p -> true).getStatus()).as("ORDER_WITHDRAWN does not cancel it").isEqualTo("Created");

        send("5678", "ORDER_CANCELLED", "2024-03-01 10:10:00", null);
        final ProcessInstance started = process("5678", p -> "Cancelled".equals(p.getStatus()));
        assertThat(started.getDefinitionRevision()).isEqualTo(before);
    }

    private ProcessInstance process(String orderId, Predicate<ProcessInstance> condition) {
        return await(() -> monitor.process("ORDER_DELIVERY", orderId), condition, "order " + orderId);
    }

    private StepInstance step(String code, Predicate<StepInstance> condition) {
        return await(() -> monitor.step("1234", code), condition, code + " reaching the expected state");
    }

    private MeasurementInstance measurement(Predicate<MeasurementInstance> condition) {
        return await(() -> monitor.measurements("1234").stream().filter(condition).findFirst(), m -> true,
                "the measurement being recorded");
    }

    private void assertActual(String step, String code, String deviation, Conformance conformance) {
        MeasurementInstance actual = await(() -> monitor.measurements("1234").stream()
                        .filter(m -> step.equals(m.getStepCode()) && code.equals(m.getCode()) && "A".equals(m.getType())
                                && !m.isInterim()).findFirst(), m -> true, "the actual " + code + " of " + step);
        assertThat(actual.getDeviation()).as(step + " " + code).isEqualTo(deviation);
        assertThat(actual.getConformance()).as(step + " " + code).isEqualTo(conformance);
    }

    private void send(String orderId, String code, String utc, String entity) {
        broker.send(TOPIC, orderId, "{\"tenantKey\":\"SHOP\",\"eventCode\":\"" + code + "\",\"entityType\":\"order\","
                + "\"entityId\":\"" + orderId + "\",\"eventUTCTime\":\"" + utc + "\""
                + (entity == null ? "" : ",\"entity\":" + entity) + "}");
    }
}
