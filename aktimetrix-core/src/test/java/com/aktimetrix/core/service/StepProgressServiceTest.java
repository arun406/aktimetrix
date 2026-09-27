package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.service.StepDefinitionService;
import com.aktimetrix.core.transferobjects.Event;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StepProgressServiceTest {

    private static final String TENANT = "AA";
    private static final LocalDateTime SHIPPED_AT = LocalDateTime.of(2022, 5, 23, 1, 30);

    @Mock
    private StepInstanceService stepInstanceService;
    @Mock
    private StepDefinitionService stepDefinitionService;
    @Mock
    private ProcessInstanceService processInstanceService;
    @Mock
    private MeasurementInstanceService measurementInstanceService;
    @Mock
    private MeasurementInstancePublisherService measurementInstancePublisherService;
    @Mock
    private StepInstancePublisherService stepInstancePublisherService;
    private StepProgressService service;

    private ProcessInstance process;

    @BeforeEach
    void setUp() {
        service = new StepProgressService(stepInstanceService, stepDefinitionService, processInstanceService,
                measurementInstanceService, measurementInstancePublisherService, stepInstancePublisherService,
                Clock.fixed(Instant.parse("2022-05-23T12:00:00Z"), ZoneOffset.UTC));
        process = new ProcessInstance();
        process.setId(new ObjectId());
        process.setTenant(TENANT);
        process.setStatus(Constants.STATUS_CREATED);
    }

    @Test
    void milestoneEventCompletesStepAndRecordsActualTime() {
        StepInstance ship = step("SHIP", Constants.STATUS_CREATED);
        givenSteps(ship, step("DELIVER", Constants.STATUS_CREATED));
        givenDefinition("SHIP", List.of("ORDER_SHIPPED_EVENT"), List.of());
        givenDefinition("DELIVER", List.of("ORDER_DELIVERED_EVENT"), List.of());

        List<MeasurementInstance> actuals = service.recordMilestone("ORDER_SHIPPED_EVENT", process, SHIPPED_AT);

        assertThat(ship.getStatus()).isEqualTo(Constants.STATUS_COMPLETED);
        assertThat(ship.getActualAt()).isEqualTo(SHIPPED_AT);
        assertThat(ship.getTimeliness()).isNull();
        verify(stepInstancePublisherService).publish(ship, "COMPLETED");
        assertThat(actuals).singleElement().satisfies(m -> {
            assertThat(m.getStepCode()).isEqualTo("SHIP");
            assertThat(m.getStepInstanceId()).isEqualTo(ship.getId());
            assertThat(m.getProcessInstanceId()).isEqualTo(process.getId());
            assertThat(m.getCode()).isEqualTo("TIME");
            assertThat(m.getType()).isEqualTo(Constants.ACTUAL_MEASUREMENT_TYPE);
            assertThat(m.getUnit()).isEqualTo("TIMESTAMP");
            assertThat(m.getValue()).isEqualTo("2022-05-23T01:30");
        });
        verify(stepInstanceService).save(ship);
        verify(measurementInstanceService).saveMeasurementInstances(actuals);
        ArgumentCaptor<Context> published = ArgumentCaptor.forClass(Context.class);
        verify(measurementInstancePublisherService).postProcess(published.capture());
        assertThat(published.getValue().getMeasurementInstances()).isEqualTo(actuals);
        assertThat(process.isComplete()).isFalse();
    }

    @Test
    void stepWithEndEventsStartsThenCompletes() {
        StepInstance ship = step("SHIP", Constants.STATUS_CREATED);
        givenSteps(ship);
        givenDefinition("SHIP", List.of("PICKED_EVENT"), List.of("SHIPPED_EVENT"));

        assertThat(service.recordMilestone("PICKED_EVENT", process, SHIPPED_AT)).isEmpty();
        assertThat(ship.getStatus()).isEqualTo(Constants.STATUS_STARTED);

        assertThat(service.recordMilestone("SHIPPED_EVENT", process, SHIPPED_AT)).hasSize(1);
        assertThat(ship.getStatus()).isEqualTo(Constants.STATUS_COMPLETED);
    }

    @Test
    void replayedEventDoesNotRecordAgain() {
        givenSteps(step("SHIP", Constants.STATUS_COMPLETED));
        givenDefinition("SHIP", List.of("ORDER_SHIPPED_EVENT"), List.of());

        assertThat(service.recordMilestone("ORDER_SHIPPED_EVENT", process, SHIPPED_AT)).isEmpty();

        verify(stepInstanceService, never()).save(any(StepInstance.class));
        verify(measurementInstanceService, never()).saveMeasurementInstances(any());
        verify(measurementInstancePublisherService, never()).postProcess(any());
    }

    @Test
    void unrelatedEventChangesNothing() {
        StepInstance ship = step("SHIP", Constants.STATUS_CREATED);
        givenSteps(ship);
        givenDefinition("SHIP", List.of("ORDER_SHIPPED_EVENT"), List.of());

        assertThat(service.recordMilestone("ORDER_CANCELLED_EVENT", process, SHIPPED_AT)).isEmpty();
        assertThat(ship.getStatus()).isEqualTo(Constants.STATUS_CREATED);
    }

    @Test
    void processCompletesWhenLastMandatoryStepCompletes() {
        givenSteps(step("PLACE", Constants.STATUS_COMPLETED), step("RATE", Constants.STATUS_CREATED),
                step("DELIVER", Constants.STATUS_CREATED));
        givenDefinition("PLACE", List.of("ORDER_PLACED_EVENT"), List.of());
        StepDefinition rate = givenDefinition("RATE", List.of("ORDER_RATED_EVENT"), List.of());
        rate.setOptionalInd("Y");
        givenDefinition("DELIVER", List.of("ORDER_DELIVERED_EVENT"), List.of());

        service.recordMilestone("ORDER_DELIVERED_EVENT", process, SHIPPED_AT);

        assertThat(process.isComplete()).isTrue();
        assertThat(process.getStatus()).isEqualTo(Constants.STATUS_COMPLETED);
        verify(processInstanceService).saveProcessInstance(process);
    }

    @Test
    void completedStepIsJudgedAgainstItsPlan() {
        StepInstance ship = step("SHIP", Constants.STATUS_CREATED);
        ship.setPlannedAt(LocalDateTime.of(2022, 5, 23, 1, 46));
        StepInstance deliver = step("DELIVER", Constants.STATUS_CREATED);
        deliver.setPlannedAt(LocalDateTime.of(2022, 5, 23, 9, 46));
        givenSteps(ship, deliver);
        givenDefinition("SHIP", List.of("ORDER_SHIPPED_EVENT"), List.of());
        givenDefinition("DELIVER", List.of("ORDER_DELIVERED_EVENT"), List.of());

        service.recordMilestone("ORDER_SHIPPED_EVENT", process, SHIPPED_AT);
        service.recordMilestone("ORDER_DELIVERED_EVENT", process, LocalDateTime.of(2022, 5, 23, 10, 30));

        assertThat(ship.getTimeliness()).isEqualTo(Timeliness.ON_TIME);
        assertThat(deliver.getTimeliness()).isEqualTo(Timeliness.LATE);
    }

    @Test
    void overdueStepThatCompletesLateBecomesLate() {
        StepInstance ship = step("SHIP", Constants.STATUS_CREATED);
        ship.setPlannedAt(LocalDateTime.of(2022, 5, 23, 1, 0));
        ship.setTimeliness(Timeliness.OVERDUE);
        givenSteps(ship);
        givenDefinition("SHIP", List.of("ORDER_SHIPPED_EVENT"), List.of());

        service.recordMilestone("ORDER_SHIPPED_EVENT", process, SHIPPED_AT);

        assertThat(ship.getTimeliness()).isEqualTo(Timeliness.LATE);
    }

    @Test
    void recordsMilestonesOnEveryActiveProcessOfTheEntity() {
        when(processInstanceService.getActiveProcessInstances(TENANT, "com.ecom.order", "1234")).thenReturn(List.of(process));
        StepInstance ship = step("SHIP", Constants.STATUS_CREATED);
        givenSteps(ship);
        givenDefinition("SHIP", List.of("ORDER_SHIPPED_EVENT"), List.of());

        assertThat(service.recordMilestones(TENANT, "com.ecom.order", "1234", "ORDER_SHIPPED_EVENT", SHIPPED_AT))
                .hasSize(1);
    }

    @Test
    void occurredAtIsConvertedToTheConfiguredZone() {
        StepProgressService inKolkata = new StepProgressService(stepInstanceService, stepDefinitionService,
                processInstanceService, measurementInstanceService, measurementInstancePublisherService,
                stepInstancePublisherService, Clock.fixed(Instant.parse("2022-05-23T12:00:00Z"), ZoneId.of("Asia/Kolkata")));
        Event<Object, Object> event = new Event<>();
        assertThat(inKolkata.occurredAt(event)).isEqualTo(LocalDateTime.of(2022, 5, 23, 17, 30));

        event.setEventUTCTime(LocalDateTime.of(2022, 5, 22, 22, 0));
        assertThat(inKolkata.occurredAt(event)).isEqualTo(LocalDateTime.of(2022, 5, 23, 3, 30));

        event.setEventTime(ZonedDateTime.of(2022, 5, 23, 0, 0, 0, 0, ZoneOffset.ofHours(2)));
        assertThat(inKolkata.occurredAt(event)).isEqualTo(LocalDateTime.of(2022, 5, 23, 3, 30));
    }

    private StepInstance step(String code, String status) {
        StepInstance step = new StepInstance(TENANT, code, process.getId(), null, null, "1.0.0", status,
                LocalDateTime.now());
        step.setId(new ObjectId());
        return step;
    }

    private void givenSteps(StepInstance... steps) {
        when(stepInstanceService.getStepInstancesByProcessInstanceId(TENANT, process.getId())).thenReturn(List.of(steps));
    }

    private StepDefinition givenDefinition(String code, List<String> startEvents, List<String> endEvents) {
        StepDefinition definition = new StepDefinition();
        definition.setStepCode(code);
        definition.setStartEventCodes(startEvents);
        definition.setEndEventCodes(endEvents);
        definition.setOptionalInd("N");
        when(stepDefinitionService.findByStepCode(eq(TENANT), eq(code))).thenReturn(definition);
        return definition;
    }
}
