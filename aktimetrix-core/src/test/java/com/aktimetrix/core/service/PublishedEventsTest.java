package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.PublishedEvents;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.outbox.Outbox;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.EventContext;
import com.aktimetrix.core.transferobjects.EventContext.Cause;
import com.aktimetrix.core.transferobjects.Measurement;
import com.aktimetrix.core.transferobjects.StepInstanceDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishedEventsTest {

    private static final LocalDateTime DELIVERED_AT = LocalDateTime.of(2024, 3, 1, 12, 55);

    @Mock
    private Outbox outbox;
    @Mock
    private ProcessInstanceService processInstanceService;
    private PublishedEventContexts contexts;
    private ProcessInstance order;

    @BeforeEach
    void setUp() {
        contexts = new PublishedEventContexts(processInstanceService,
                Clock.fixed(Instant.parse("2024-03-01T13:00:00Z"), ZoneOffset.UTC));
        ProcessDefinition definition = new ProcessDefinition("AA", "ORDER_DELIVERY");
        definition.setProcessName("Order delivery");
        definition.setRevision(3L);
        StepDefinition rated = new StepDefinition();
        rated.setStepCode("RATED");
        rated.setStepName("Rated by the customer");
        rated.setOptionalInd("Y");
        definition.setSteps(List.of(rated));
        order = new ProcessInstance(definition);
        order.setId("p-1");
        order.setEntityType("com.ecom.order");
        order.setEntityId("1234");
        order.setRevision(4L);
    }

    @Test
    void aStepEventTellsWhichEntityAndProcessItBelongsToAndWhatCausedIt() {
        when(processInstanceService.getProcessInstance("AA", "p-1")).thenReturn(order);
        StepInstance rated = new StepInstance("AA", "RATED", "p-1", null, null, "1.0.0", "Completed", DELIVERED_AT);
        rated.setId("s-7");
        rated.setSequence(6);
        rated.setRevision(2L);
        StepInstancePublisherService publisher = new StepInstancePublisherService(outbox, contexts);

        ProcessingContext.run(new Cause(Cause.EVENT, "e-42", "ORDER_RATED_EVENT"), DELIVERED_AT,
                () -> publisher.publish(rated, PublishedEvents.Step.COMPLETED));

        Event<StepInstanceDTO, EventContext> event = captured("step-instance-out-0", "p-1");
        assertThat(event.getEventType()).isEqualTo("Step_Event");
        assertThat(event.getEventName()).isEqualTo("Step completed");
        assertThat(event.getSource()).isEqualTo("aktimetrix");
        assertThat(event.getEntityId()).isEqualTo("s-7");
        assertThat(event.getEntity().getStepName()).isEqualTo("Rated by the customer");
        assertThat(event.getEntity().isOptional()).isTrue();
        EventContext context = event.getEventDetails();
        assertThat(context.getSchemaVersion()).isEqualTo("1");
        assertThat(context.getBusinessEntity()).isEqualTo(new EventContext.BusinessEntity("com.ecom.order", "1234"));
        assertThat(context.getProcessCode()).isEqualTo("ORDER_DELIVERY");
        assertThat(context.getProcessInstanceId()).isEqualTo("p-1");
        assertThat(context.getDefinitionRevision()).isEqualTo(3L);
        assertThat(context.getStepInstanceId()).isEqualTo("s-7");
        assertThat(context.getRevision()).isEqualTo(2L);
        assertThat(context.getOccurredAt()).isEqualTo(DELIVERED_AT);
        assertThat(context.getCause()).isEqualTo(new Cause(Cause.EVENT, "e-42", "ORDER_RATED_EVENT"));
    }

    @Test
    void aMeasurementEventSaysWhatKindOfMeasurementItIs() {
        when(processInstanceService.getProcessInstance("AA", "p-1")).thenReturn(order);
        MeasurementInstancePublisherService publisher = new MeasurementInstancePublisherService(outbox, contexts);
        MeasurementInstance reading = measurement("DISTANCE", Constants.ACTUAL_MEASUREMENT_TYPE);
        reading.setInterim(true);

        publisher.publish(reading);

        Event<Measurement, EventContext> event = captured("measurement-instance-out-0", "p-1");
        assertThat(event.getEventCode()).isEqualTo(PublishedEvents.Measurement.READING);
        assertThat(event.getEventDetails().getOccurredAt()).as("outside a unit of work: now")
                .isEqualTo(LocalDateTime.of(2024, 3, 1, 13, 0));
        assertThat(event.getEventDetails().getCause()).isNull();
    }

    @Test
    void measurementCodes() {
        MeasurementInstance planned = measurement("DISTANCE", Constants.PLAN_MEASUREMENT_TYPE);
        MeasurementInstance actual = measurement("DISTANCE", Constants.ACTUAL_MEASUREMENT_TYPE);
        MeasurementInstance metric = measurement("FUEL_PER_KM", Constants.ACTUAL_MEASUREMENT_TYPE);
        metric.setDerivedFrom("FUEL / DISTANCE");

        assertThat(com.aktimetrix.core.impl.MeasurementEventGenerator.codeOf(planned)).isEqualTo("PLANNED");
        assertThat(com.aktimetrix.core.impl.MeasurementEventGenerator.codeOf(actual)).isEqualTo("RECORDED");
        assertThat(com.aktimetrix.core.impl.MeasurementEventGenerator.codeOf(metric)).isEqualTo("METRIC");
    }

    @Test
    void aProcessEventCarriesItsDefinitionRevisionAndIsKeyedByProcess() {
        ProcessInstancePublisherService publisher = new ProcessInstancePublisherService(outbox, contexts);

        ProcessingContext.run(new Cause(Cause.DEADLINE, null, null), LocalDateTime.of(2024, 3, 2, 9, 1),
                () -> publisher.publish(order, PublishedEvents.Process.OVERDUE));

        Event<Map<String, Object>, EventContext> event = captured("process-instance-out-0", "p-1");
        EventContext context = event.getEventDetails();
        assertThat(context.getRevision()).isEqualTo(4L);
        assertThat(context.getCause().getType()).isEqualTo(Cause.DEADLINE);
        assertThat(context.getOccurredAt()).isEqualTo(LocalDateTime.of(2024, 3, 2, 9, 1));
    }

    private MeasurementInstance measurement(String code, String type) {
        MeasurementInstance measurement = new MeasurementInstance("AA", code, "8", "KM", "p-1", "s-5", "TRAVEL", type,
                null, ZonedDateTime.now(ZoneOffset.UTC));
        measurement.setId("m-1");
        return measurement;
    }

    @SuppressWarnings("unchecked")
    private <T> Event<T, EventContext> captured(String binding, String key) {
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(outbox).enqueue(eq(binding), eq(key), event.capture());
        return (Event<T, EventContext>) event.getValue();
    }
}
