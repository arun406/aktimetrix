package com.aktimetrix.it;

import com.aktimetrix.core.api.Conformance;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import com.aktimetrix.core.service.ProcessMigrationService;
import com.aktimetrix.core.store.ProcessInstanceStore;
import com.aktimetrix.core.store.StepInstanceStore;
import com.aktimetrix.it.support.TestBroker;
import com.aktimetrix.it.support.TestMonitor;
import com.aktimetrix.it.support.TestStore;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.aktimetrix.it.support.TestMonitor.await;
import static com.aktimetrix.it.support.TestMonitor.awaitTrue;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The smallest possible monitor, {@link ParcelMonitor}: JSON definitions ({@code src/test/resources/aktimetrix}) and
 * two meters, one per step and one per process, with no {@code @ComponentScan}, process handler or event handler.
 * Each subclass runs it on one message broker and one state store.
 * <p>
 * A parcel is booked at 09:00. PICKUP is planned by a meter (+1 h, 10 minutes' tolerance), SORT by a duration from the
 * start (+3 h), and DELIVER 5 h after SORT completes; DELIVER is defined only in the process, not as a shared step.
 * The process as a whole has a planned DISTANCE and should take 12 hours; the parcel is cancelled before delivery.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class ParcelScenario {

    private static final String TOPIC = "parcel-events";
    private static final Instant BOOKED = LocalDateTime.of(2024, 1, 10, 9, 0).toInstant(ZoneOffset.UTC);
    private final ObjectMapper json = new ObjectMapper();

    private TestBroker broker;
    private TestStore store;
    private TestMonitor monitor;

    protected abstract TestBroker broker(String eventsTopic);

    protected abstract TestStore store();

    @BeforeAll
    void start() {
        broker = broker(TOPIC);
        store = store();
        monitor = new TestMonitor(ParcelMonitor.class, "T1", broker, store, "aktimetrix.events.topic=" + TOPIC);
    }

    @AfterAll
    void stop() {
        monitor.close();
        store.close();
        broker.close();
    }

    @Test
    void plansByMeterAndByDurationForecastsRiskAndPublishesThroughTheOutbox() throws Exception {
        // booked at 09:00: PICKUP planned 10:00 by the meter, SORT 12:00 by duration, DELIVER not yet
        send("PARCEL_BOOKED", "2024-01-10 09:00:00",
                "{\"bookedAt\":\"2024-01-10 09:00:00\",\"from\":\"AMS\",\"to\":\"RTM\"}");
        StepInstance pickup = step("PICKUP", s -> s.getLateAfter() != null);
        assertThat(pickup.getPlannedAt()).isEqualTo(BOOKED.plus(Duration.ofHours(1)));
        assertThat(pickup.getLateAfter()).isEqualTo(BOOKED.plus(Duration.ofHours(1)).plus(Duration.ofMinutes(10)));
        StepInstance sort = step("SORT", s -> s.getPlannedAt() != null);
        assertThat(sort.getPlannedAt()).isEqualTo(BOOKED.plus(Duration.ofHours(3)));
        assertThat(monitor.step("P-1", "DELIVER").orElseThrow().getPlannedAt()).isNull();
        assertThat(step("BOOK", s -> "Completed".equals(s.getStatus())).getActualAt()).isEqualTo(BOOKED);

        // the process-level meter planned the parcel's distance, on the process rather than on a step
        MeasurementInstance distance = measurement(m -> "DISTANCE".equals(m.getCode()));
        assertThat(distance.getValue()).isEqualTo("78");
        assertThat(distance.getUnit()).isEqualTo("KM");
        assertThat(distance.getType()).isEqualTo("P");
        assertThat(distance.getProcessInstanceId()).isEqualTo(pickup.getProcessInstanceId());
        assertThat(distance.getStepInstanceId()).isNull();

        // picked up at 11:30, 90 minutes late: SORT is forecast for 13:30, past its 12:00 deadline
        send("PARCEL_PICKED_UP", "2024-01-10 11:30:00", null);
        pickup = step("PICKUP", s -> "Completed".equals(s.getStatus()));
        assertThat(pickup.getTimeliness()).isEqualTo(Timeliness.LATE);
        sort = step("SORT", s -> s.getTimeliness() != null);
        assertThat(sort.getTimeliness()).isEqualTo(Timeliness.AT_RISK);
        assertThat(sort.getExpectedAt()).isEqualTo(BOOKED.plus(Duration.ofHours(4)).plus(Duration.ofMinutes(30)));

        // sorted at 12:10: late, and DELIVER is planned 5 hours later
        send("PARCEL_SORTED", "2024-01-10 12:10:00", "{\"scale\":{\"weightKg\":2.5}}");
        sort = step("SORT", s -> "Completed".equals(s.getStatus()));
        assertThat(sort.getTimeliness()).isEqualTo(Timeliness.LATE);
        assertThat(step("DELIVER", s -> s.getPlannedAt() != null).getPlannedAt())
                .isEqualTo(LocalDateTime.of(2024, 1, 10, 17, 10).toInstant(ZoneOffset.UTC));

        // the sorting event carried the parcel's weight: SORT's actual WEIGHT, compared with the planned 2 kg ± 10%
        MeasurementInstance weight = measurement(m -> "WEIGHT".equals(m.getCode()) && "A".equals(m.getType()));
        assertThat(weight.getValue()).isEqualTo("2.5");
        assertThat(weight.getUnit()).isEqualTo("KG");
        assertThat(weight.getStepCode()).isEqualTo("SORT");
        assertThat(weight.getPlannedValue()).isEqualTo("2");
        assertThat(weight.getDeviation()).isEqualTo("0.5");
        assertThat(weight.getConformance()).isEqualTo(Conformance.OUT_OF_TOLERANCE);

        // SORT's actual TIME is compared with its plan too: 10 minutes late
        MeasurementInstance sortTime = measurement(m -> "TIME".equals(m.getCode()) && "A".equals(m.getType())
                && "SORT".equals(m.getStepCode()));
        assertThat(sortTime.getPlannedValue()).isEqualTo("2024-01-10T12:00:00Z");
        assertThat(sortTime.getDeviation()).isEqualTo("PT10M");

        // the process has its own deadline: 12 hours after booking
        ProcessInstance parcel = monitor.process("PARCEL", "P-1").orElseThrow();
        assertThat(parcel.getPlannedAt()).isEqualTo(BOOKED.plus(Duration.ofHours(12)));

        // cancelled before delivery: the process ends, and DELIVER is no longer awaited
        send("PARCEL_CANCELLED", "2024-01-10 13:00:00", null);
        assertThat(step("DELIVER", s -> "Cancelled".equals(s.getStatus())).getActualAt()).isNull();
        parcel = monitor.process("PARCEL", "P-1").orElseThrow();
        assertThat(parcel.getStatus()).isEqualTo("Cancelled");
        assertThat(parcel.getEndedAt()).isEqualTo(LocalDateTime.of(2024, 1, 10, 13, 0).toInstant(ZoneOffset.UTC));

        // the steps that needed attention were notified, after the outbox relayed them
        final RecordingNotifier notifier = monitor.bean(RecordingNotifier.class);
        awaitTrue(() -> notifier.received().stream().anyMatch(n -> "LATE".equals(n.getCondition())
                && "SORT".equals(n.getStepCode())), "SORT completing late being notified");
        assertThat(notifier.received()).filteredOn(n -> "P-1".equals(n.getEntityId()))
                .extracting(n -> n.getStepCode() + " " + n.getCondition())
                .contains("SORT AT_RISK", "PICKUP LATE", "SORT LATE");

        // every change reached the broker through the outbox
        Set<String> expected = Set.of("PICKUP CREATED", "SORT AT_RISK", "DELIVER PLANNED", "SORT COMPLETED",
                "DELIVER CANCELLED");
        assertThat(stepEventsPublished(expected)).containsAll(expected);
        awaitTrue(() -> monitor.pendingOutbox() == 0, "the outbox emptied");

        assertThat(monitor.meters().get("aktimetrix.processes.started").counter().count()).isEqualTo(1);
        assertThat(monitor.meters().get("aktimetrix.steps.at.risk").counter().count()).isEqualTo(1);
        assertThat(total(monitor.meters().get("aktimetrix.steps.completed").tag("timeliness", "LATE").counters()))
                .as("PICKUP and SORT").isEqualTo(2);
        assertThat(monitor.meters().get("aktimetrix.processes.cancelled").counter().count()).isEqualTo(1);
        assertThat(total(monitor.meters().get("aktimetrix.events").tag("outcome", "handled").counters())).isEqualTo(4);
    }

    /**
     * An event delivered twice is processed once; an event dated in the future is invalid.
     */
    @Test
    @Order(Integer.MAX_VALUE - 1)   // after the scenario whose counters it would change
    void ignoresDuplicateEventsAndRejectsEventsDatedInTheFuture() {
        sendFor("P-6", "booking-6", "PARCEL_BOOKED", "2024-01-12 09:00:00", "{\"bookedAt\":\"2024-01-12 09:00:00\"}");
        sendFor("P-6", "pickup-6", "PARCEL_PICKED_UP", "2024-01-12 10:00:00", null);
        sendFor("P-6", "pickup-6", "PARCEL_PICKED_UP", "2024-01-12 10:00:00", null);
        awaitTrue(() -> total(monitor.meters().find("aktimetrix.events").tag("outcome", "duplicate").counters()) == 1,
                "the duplicate being recognised");

        final String future = "{\"tenantKey\":\"T1\",\"eventId\":\"future-6\",\"eventCode\":\"PARCEL_SORTED\","
                + "\"entityType\":\"parcel\",\"entityId\":\"P-6\",\"eventUTCTime\":\"2999-01-01 00:00:00\"}";
        broker.send(TOPIC, "P-6", future);
        assertThat(broker.received(TOPIC + ".dlq", messages -> messages.contains(future))).contains(future);
        assertThat(stepOf(runs("P-6").get(0), "SORT").orElseThrow().getStatus()).as("the future event changed nothing")
                .isNotEqualTo("Completed");
    }

    /**
     * The process is restartable: a parcel booked again after its run was cancelled starts a second run, which later
     * events apply to; a replay of the booking that started the first run starts nothing.
     */
    @Test
    @Order(Integer.MAX_VALUE)   // last: its runs would change the counters the other scenarios check
    void aRestartableProcessStartsANewRunAfterTheLatestEnded() {
        sendFor("P-5", "booking-1", "PARCEL_BOOKED", "2024-01-11 09:00:00", "{\"bookedAt\":\"2024-01-11 09:00:00\"}");
        await(() -> monitor.process("PARCEL", "P-5"), p -> p.getId() != null, "run 1 starting");
        sendFor("P-5", "cancel-1", "PARCEL_CANCELLED", "2024-01-11 10:00:00", null);
        await(() -> monitor.process("PARCEL", "P-5"), p -> "Cancelled".equals(p.getStatus()), "run 1 cancelled");

        sendFor("P-5", "booking-1", "PARCEL_BOOKED", "2024-01-11 09:00:00", "{\"bookedAt\":\"2024-01-11 09:00:00\"}");
        sendFor("P-5", "booking-2", "PARCEL_BOOKED", "2024-01-11 11:00:00", "{\"bookedAt\":\"2024-01-11 11:00:00\"}");
        awaitTrue(() -> runs("P-5").size() == 2, "run 2 starting");
        sendFor("P-5", "pickup-2", "PARCEL_PICKED_UP", "2024-01-11 12:00:00", null);

        final List<ProcessInstance> runs = runs("P-5");
        assertThat(runs).extracting(ProcessInstance::getRun).containsExactly(1, 2);
        assertThat(runs.get(1).getStartEventId()).isEqualTo("booking-2");
        assertThat(runs.get(1).getStartedAt()).isEqualTo(LocalDateTime.of(2024, 1, 11, 11, 0).toInstant(ZoneOffset.UTC));
        awaitTrue(() -> stepOf(runs.get(1), "PICKUP").map(s -> "Completed".equals(s.getStatus())).orElse(false),
                "PICKUP of run 2 completing");
        assertThat(stepOf(runs.get(0), "PICKUP").orElseThrow().getStatus()).as("run 1 has ended")
                .isEqualTo("Cancelled");
    }

    /**
     * A revision of the return process adds a labelling step: the return already running is migrated to it, and gets
     * the new step, planned from its start.
     */
    @Test
    @Order(Integer.MAX_VALUE - 3)   // before the return scenario, which then follows the new revision
    void migratesARunningInstanceToTheCurrentRevision() {
        sendFor("return", "R-2", "return-2", "RETURN_REQUESTED", "2024-01-13 09:00:00", "{}");
        final ProcessInstance started = await(() -> monitor.process("RETURN", "R-2"), p -> p.getId() != null,
                "the return starting");
        final ProcessDefinitionService definitions = monitor.bean(ProcessDefinitionService.class);
        final ProcessDefinition revised = definitions.currentDefinition("T1", "RETURN");
        final StepDefinition label = new StepDefinition();
        label.setStepCode("LABEL");
        label.setStartEventCodes(List.of("RETURN_LABELLED"));
        label.setOptionalInd("Y");
        label.setPlannedWithin("PT2H");
        final List<StepDefinition> steps = new ArrayList<>(revised.getSteps());
        steps.add(label);
        revised.setSteps(steps);
        final long revision = definitions.add(revised).getRevision();

        final ProcessMigrationService.Migration migration = monitor.bean(ProcessMigrationService.class)
                .migrate("T1", "RETURN");

        assertThat(migration.getMigrated()).containsExactly(started.getId());
        assertThat(migration.getRevision()).isEqualTo(revision);
        assertThat(monitor.process("RETURN", "R-2").orElseThrow().getDefinitionRevision()).isEqualTo(revision);
        assertThat(monitor.step("R-2", "LABEL").orElseThrow().getPlannedAt())
                .isEqualTo(LocalDateTime.of(2024, 1, 13, 11, 0).toInstant(ZoneOffset.UTC));
        assertThat(monitor.bean(ProcessMigrationService.class).migrate("T1", "RETURN").getUpToDate()).isEqualTo(1);
    }

    /**
     * A return's inspection is repeatable, and it is settled by a refund or a replacement: each inspection is an
     * attempt, and the refund skips the replacement, so the return completes without it.
     */
    @Test
    @Order(Integer.MAX_VALUE - 2)   // after the scenario whose counters it would change
    void recordsRepeatedStepsAndTakesOneOfTheAlternatives() {
        sendFor("return", "R-1", "return-1", "RETURN_REQUESTED", "2024-01-12 09:00:00", "{}");
        await(() -> monitor.process("RETURN", "R-1"), p -> p.getId() != null, "the return starting");
        sendFor("return", "R-1", "inspect-1", "RETURN_INSPECTED", "2024-01-12 10:00:00", null);
        sendFor("return", "R-1", "inspect-2", "RETURN_INSPECTED", "2024-01-12 11:00:00", null);
        sendFor("return", "R-1", "refund-1", "RETURN_REFUNDED", "2024-01-12 12:00:00", null);

        final StepInstance inspect = await(() -> monitor.step("R-1", "INSPECT"), s -> s.getAttempts() == 2,
                "the second inspection");
        assertThat(inspect.getActualAt()).isEqualTo(LocalDateTime.of(2024, 1, 12, 10, 0).toInstant(ZoneOffset.UTC));
        assertThat(inspect.getLastAttemptAt()).isEqualTo(LocalDateTime.of(2024, 1, 12, 11, 0).toInstant(ZoneOffset.UTC));
        await(() -> monitor.step("R-1", "REPLACE"), s -> "Skipped".equals(s.getStatus()), "the replacement skipped");
        await(() -> monitor.process("RETURN", "R-1"), ProcessInstance::isComplete, "the return completing");
    }

    private List<ProcessInstance> runs(String entityId) {
        return monitor.bean(ProcessInstanceStore.class).findByEntityId("T1", entityId).stream()
                .filter(p -> "PARCEL".equals(p.getProcessCode()))
                .sorted(Comparator.comparingInt(ProcessInstance::getRun))
                .collect(Collectors.toList());
    }

    private Optional<StepInstance> stepOf(ProcessInstance run, String stepCode) {
        return monitor.bean(StepInstanceStore.class).findByProcessInstance("T1", run.getId()).stream()
                .filter(s -> stepCode.equals(s.getStepCode())).findFirst();
    }

    private void sendFor(String entityId, String eventId, String eventCode, String utcTime, String entity) {
        sendFor("parcel", entityId, eventId, eventCode, utcTime, entity);
    }

    private void sendFor(String entityType, String entityId, String eventId, String eventCode, String utcTime,
                         String entity) {
        broker.send(TOPIC, entityId, "{\"tenantKey\":\"T1\",\"eventId\":\"" + eventId + "\",\"eventCode\":\""
                + eventCode + "\",\"entityType\":\"" + entityType + "\",\"entityId\":\"" + entityId + "\",\"eventUTCTime\":\""
                + utcTime + "\"" + (entity == null ? "" : ",\"entity\":" + entity) + "}");
    }

    /**
     * Invalid events can never succeed, and go straight to the dead-letter channel; an event whose processing fails
     * is retried by the binder, then sent there too.
     */
    @Test
    void sendsInvalidAndFailingEventsToTheDeadLetterChannel() {
        final String poison = "{\"tenantKey\":\"T1\",\"eventCode\":\"PARCEL_POISON\",\"entityType\":\"parcel\","
                + "\"entityId\":\"P-3\",\"eventUTCTime\":\"2024-01-10 09:00:00\"}";
        final List<String> expected = List.of("this is not JSON",
                "{\"eventCode\":\"PARCEL_BOOKED\",\"entityId\":\"P-2\"}", poison);
        broker.send(TOPIC, "P-2", expected.get(0));
        broker.send(TOPIC, "P-2", expected.get(1));   // no tenant
        broker.send(TOPIC, "P-3", poison);

        assertThat(broker.received(TOPIC + ".dlq", messages -> messages.containsAll(expected)))
                .containsAll(expected);
        assertThat(total(monitor.meters().get("aktimetrix.events").tag("outcome", "invalid").counters())).isEqualTo(2);
        assertThat(total(monitor.meters().get("aktimetrix.events").tag("outcome", "failed").counters()))
                .as("each attempt").isEqualTo(3);
    }

    private String describe(String message) {
        try {
            final JsonNode event = json.readTree(message);
            return event.get("entity").get("stepCode").asText() + " " + event.get("eventCode").asText();
        } catch (Exception e) {
            throw new AssertionError("Not a step event: " + message, e);
        }
    }

    private static double total(Collection<Counter> counters) {
        return counters.stream().mapToDouble(Counter::count).sum();
    }

    private void send(String eventCode, String utcTime, String entity) {
        broker.send(TOPIC, "P-1", "{\"tenantKey\":\"T1\",\"eventCode\":\"" + eventCode + "\",\"entityType\":\"parcel\","
                + "\"entityId\":\"P-1\",\"eventUTCTime\":\"" + utcTime + "\""
                + (entity == null ? "" : ",\"entity\":" + entity) + "}");
    }

    private StepInstance step(String code, Predicate<StepInstance> condition) {
        return await(() -> monitor.step("P-1", code), condition, code + " reaching the expected state");
    }

    private MeasurementInstance measurement(Predicate<MeasurementInstance> condition) {
        return await(() -> monitor.measurements("P-1").stream().filter(condition).findFirst(), m -> true,
                "the measurement being recorded");
    }

    /**
     * "STEP EVENTCODE" of every step event published.
     */
    private Set<String> stepEventsPublished(Set<String> expected) {
        final Set<String> events = new HashSet<>();
        broker.received("step-instance-out-0", messages -> {
            events.clear();
            messages.forEach(message -> events.add(describe(message)));
            return events.containsAll(expected);
        });
        return events;
    }
}
