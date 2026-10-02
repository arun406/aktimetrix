package com.aktimetrix.core.service;

import java.util.UUID;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.store.DefinitionStore;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import com.aktimetrix.core.transferobjects.Event;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private ProcessInstanceService processInstanceService;
    @Mock
    private MeasurementInstanceService measurementInstanceService;
    @Mock
    private MeasurementInstancePublisherService measurementInstancePublisherService;
    @Mock
    private StepInstancePublisherService stepInstancePublisherService;
    @Mock
    private DefinitionStore definitionStore;
    private ProcessDefinitionService processDefinitionService;
    private ProcessDefinition processDefinition;
    @Mock
    private ProcessInstancePublisherService processInstancePublisherService;
    @Mock
    private ActualMeasurementService actualMeasurementService;
    @Mock
    private DerivedMetricService derivedMetricService;
    private StepProgressService service;
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private int nextSequence;

    private ProcessInstance process;

    @BeforeEach
    void setUp() {
        processDefinitionService = new ProcessDefinitionService(definitionStore);
        service = new StepProgressService(stepInstanceService, processInstanceService,
                measurementInstanceService, measurementInstancePublisherService, stepInstancePublisherService,
                new StepPlanner(), metrics(registry), Clock.fixed(Instant.parse("2022-05-23T12:00:00Z"), ZoneOffset.UTC),
                processDefinitionService, processInstancePublisherService, actualMeasurementService,
                derivedMetricService);
        process = new ProcessInstance();
        process.setId(UUID.randomUUID().toString());
        process.setTenant(TENANT);
        process.setStatus(Constants.STATUS_CREATED);
        useDefinition(new ProcessDefinition(TENANT, "ORDER_DELIVERY"));
    }

    /**
     * The definition the process instance started with, keeping the steps defined so far.
     */
    private void useDefinition(ProcessDefinition definition) {
        definition.setSteps(processDefinition == null ? new ArrayList<>() : processDefinition.getSteps());
        processDefinition = definition;
        process.setDefinition(definition);
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
        assertThat(process.getEndedAt()).isEqualTo(SHIPPED_AT);
        assertThat(process.getTimeliness()).as("no deadline, no timeliness").isNull();
        verify(processInstanceService).saveProcessInstance(process);
        verify(processInstancePublisherService).publish(process, "COMPLETED");
    }

    @Test
    void processWithADeadlineIsJudgedWhenItCompletes() {
        process.setLateAfter(SHIPPED_AT.minusMinutes(1));
        givenSteps(step("DELIVER", Constants.STATUS_CREATED));
        givenDefinition("DELIVER", List.of("ORDER_DELIVERED_EVENT"), List.of());

        service.recordMilestone("ORDER_DELIVERED_EVENT", process, SHIPPED_AT);

        assertThat(process.getTimeliness()).isEqualTo(Timeliness.LATE);
    }

    @Test
    void cancelEventCancelsTheProcessAndItsOpenSteps() {
        process.setProcessCode("ORDER_DELIVERY");
        ProcessDefinition definition = new ProcessDefinition(TENANT, "ORDER_DELIVERY");
        definition.setCancelEventCodes(List.of("ORDER_CANCELLED_EVENT"));
        when(processInstanceService.getCurrentRuns(TENANT, "com.ecom.order", "1234")).thenReturn(List.of(process));
        useDefinition(definition);
        StepInstance place = step("PLACE", Constants.STATUS_COMPLETED);
        StepInstance ship = step("SHIP", Constants.STATUS_CREATED);
        givenSteps(place, ship);

        service.recordMilestones(TENANT, "com.ecom.order", "1234", "ORDER_CANCELLED_EVENT", SHIPPED_AT);

        assertThat(process.getStatus()).isEqualTo(Constants.STATUS_CANCELLED);
        assertThat(process.isComplete()).as("no longer active").isTrue();
        assertThat(process.getEndedAt()).isEqualTo(SHIPPED_AT);
        assertThat(place.getStatus()).isEqualTo(Constants.STATUS_COMPLETED);
        assertThat(ship.getStatus()).isEqualTo(Constants.STATUS_CANCELLED);
        verify(processInstancePublisherService).publish(process, "CANCELLED");
        verify(stepInstancePublisherService).publish(ship, "CANCELLED");
        verify(stepInstancePublisherService, never()).publish(place, "CANCELLED");
    }

    @Test
    void anOptionalStepIsStillRecordedAfterItsProcessCompleted() {
        // delivered, so the process is complete; the customer rates it the next day
        process.setComplete(true);
        process.setStatus(Constants.STATUS_COMPLETED);
        StepInstance rated = step("RATED", Constants.STATUS_CREATED);
        givenSteps(step("DELIVER", Constants.STATUS_COMPLETED), rated);
        givenDefinition("DELIVER", List.of("ORDER_DELIVERED_EVENT"), List.of());
        givenDefinition("RATED", List.of("ORDER_RATED_EVENT"), List.of()).setOptionalInd("Y");
        when(processInstanceService.getCurrentRuns(TENANT, "com.ecom.order", "1234")).thenReturn(List.of(process));

        service.recordMilestones(TENANT, "com.ecom.order", "1234", "ORDER_RATED_EVENT", SHIPPED_AT);

        assertThat(rated.getStatus()).isEqualTo(Constants.STATUS_COMPLETED);
        verify(processInstancePublisherService, never()).publish(process, "COMPLETED");
    }

    @Test
    void anExplicitEndEventCompletesTheProcessAndSkipsItsOpenMandatorySteps() {
        // the order is closed before it was delivered, and before the customer rated it
        process.setProcessCode("ORDER_DELIVERY");
        ProcessDefinition definition = new ProcessDefinition(TENANT, "ORDER_DELIVERY");
        definition.setEndEventCodes(List.of("ORDER_CLOSED_EVENT"));
        when(processInstanceService.getCurrentRuns(TENANT, "com.ecom.order", "1234")).thenReturn(List.of(process));
        useDefinition(definition);
        StepInstance place = step("PLACE", Constants.STATUS_COMPLETED);
        StepInstance deliver = step("DELIVER", Constants.STATUS_CREATED);
        StepInstance rated = step("RATED", Constants.STATUS_CREATED);
        givenSteps(place, deliver, rated);
        givenDefinition("PLACE", List.of("ORDER_PLACED_EVENT"), List.of());
        givenDefinition("DELIVER", List.of("ORDER_DELIVERED_EVENT"), List.of());
        givenDefinition("RATED", List.of("ORDER_RATED_EVENT"), List.of()).setOptionalInd("Y");

        service.recordMilestones(TENANT, "com.ecom.order", "1234", "ORDER_CLOSED_EVENT", SHIPPED_AT);

        assertThat(process.getStatus()).isEqualTo(Constants.STATUS_COMPLETED);
        assertThat(process.getEndedAt()).isEqualTo(SHIPPED_AT);
        assertThat(deliver.getStatus()).isEqualTo(Constants.STATUS_SKIPPED);
        assertThat(rated.getStatus()).as("optional: may still happen").isEqualTo(Constants.STATUS_CREATED);
        verify(stepInstancePublisherService).publish(deliver, "SKIPPED");
        verify(processInstancePublisherService).publish(process, "COMPLETED");
    }

    @Test
    void withExplicitEndEventsTheLastStepDoesNotCompleteTheProcess() {
        process.setProcessCode("ORDER_DELIVERY");
        ProcessDefinition definition = new ProcessDefinition(TENANT, "ORDER_DELIVERY");
        definition.setEndEventCodes(List.of("ORDER_CLOSED_EVENT"));
        useDefinition(definition);
        givenSteps(step("DELIVER", Constants.STATUS_CREATED));
        givenDefinition("DELIVER", List.of("ORDER_DELIVERED_EVENT"), List.of());

        service.recordMilestone("ORDER_DELIVERED_EVENT", process, SHIPPED_AT);

        assertThat(process.isComplete()).as("waits for ORDER_CLOSED_EVENT").isFalse();
    }

    @Test
    void aCompletedProcessCannotBeCancelled() {
        process.setComplete(true);
        process.setStatus(Constants.STATUS_COMPLETED);
        process.setProcessCode("ORDER_DELIVERY");
        ProcessDefinition definition = new ProcessDefinition(TENANT, "ORDER_DELIVERY");
        definition.setCancelEventCodes(List.of("ORDER_CANCELLED_EVENT"));
        when(processInstanceService.getCurrentRuns(TENANT, "com.ecom.order", "1234")).thenReturn(List.of(process));
        useDefinition(definition);

        service.recordMilestones(TENANT, "com.ecom.order", "1234", "ORDER_CANCELLED_EVENT", SHIPPED_AT);

        assertThat(process.getStatus()).isEqualTo(Constants.STATUS_COMPLETED);
        verify(processInstancePublisherService, never()).publish(process, "CANCELLED");
    }

    @Test
    void processPastItsDeadlineIsMarkedOverdue() {
        service.markProcessOverdue(process);

        assertThat(process.getTimeliness()).isEqualTo(Timeliness.OVERDUE);
        verify(processInstanceService).saveProcessInstance(process);
        verify(processInstancePublisherService).publish(process, "OVERDUE");
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
        when(processInstanceService.getCurrentRuns(TENANT, "com.ecom.order", "1234")).thenReturn(List.of(process));
        StepInstance ship = step("SHIP", Constants.STATUS_CREATED);
        givenSteps(ship);
        givenDefinition("SHIP", List.of("ORDER_SHIPPED_EVENT"), List.of());

        assertThat(service.recordMilestones(TENANT, "com.ecom.order", "1234", "ORDER_SHIPPED_EVENT", SHIPPED_AT))
                .hasSize(1);
    }

    @Test
    void occurredAtIsConvertedToTheConfiguredZone() {
        StepProgressService inKolkata = new StepProgressService(stepInstanceService,
                processInstanceService, measurementInstanceService, measurementInstancePublisherService,
                stepInstancePublisherService, new StepPlanner(), metrics(registry),
                Clock.fixed(Instant.parse("2022-05-23T12:00:00Z"), ZoneId.of("Asia/Kolkata")),
                processDefinitionService, processInstancePublisherService, actualMeasurementService,
                derivedMetricService);
        Event<Object, Object> event = new Event<>();
        assertThat(inKolkata.occurredAt(event)).isEqualTo(LocalDateTime.of(2022, 5, 23, 17, 30));

        event.setEventUTCTime(LocalDateTime.of(2022, 5, 22, 22, 0));
        assertThat(inKolkata.occurredAt(event)).isEqualTo(LocalDateTime.of(2022, 5, 23, 3, 30));

        event.setEventTime(ZonedDateTime.of(2022, 5, 23, 0, 0, 0, 0, ZoneOffset.ofHours(2)));
        assertThat(inKolkata.occurredAt(event)).isEqualTo(LocalDateTime.of(2022, 5, 23, 3, 30));
    }

    @Test
    void lateStepPutsLaterStepsAtRisk() {
        StepInstance ship = planned(step("SHIP", Constants.STATUS_CREATED), LocalDateTime.of(2022, 5, 23, 1, 46));
        StepInstance deliver = planned(step("DELIVER", Constants.STATUS_CREATED), LocalDateTime.of(2022, 5, 23, 9, 46));
        givenSteps(ship, deliver);
        givenDefinition("SHIP", List.of("ORDER_SHIPPED_EVENT"), List.of());
        givenDefinition("DELIVER", List.of("ORDER_DELIVERED_EVENT"), List.of());

        service.recordMilestone("ORDER_SHIPPED_EVENT", process, LocalDateTime.of(2022, 5, 23, 3, 0));

        assertThat(ship.getTimeliness()).isEqualTo(Timeliness.LATE);
        assertThat(deliver.getExpectedAt()).isEqualTo(LocalDateTime.of(2022, 5, 23, 11, 0));
        assertThat(deliver.getTimeliness()).isEqualTo(Timeliness.AT_RISK);
        verify(stepInstancePublisherService).publish(deliver, "AT_RISK");
        assertThat(registry.get("aktimetrix.steps.at.risk").counter().count()).isEqualTo(1);
        assertThat(registry.get("aktimetrix.steps.completed").tag("timeliness", "LATE").counter().count()).isEqualTo(1);
    }

    @Test
    void completionWithinToleranceIsOnTime() {
        StepInstance ship = planned(step("SHIP", Constants.STATUS_CREATED), LocalDateTime.of(2022, 5, 23, 1, 46));
        ship.setLateAfter(LocalDateTime.of(2022, 5, 23, 2, 1));
        givenSteps(ship);
        givenDefinition("SHIP", List.of("ORDER_SHIPPED_EVENT"), List.of());

        service.recordMilestone("ORDER_SHIPPED_EVENT", process, LocalDateTime.of(2022, 5, 23, 2, 0));

        assertThat(ship.getTimeliness()).isEqualTo(Timeliness.ON_TIME);
    }

    @Test
    void aStepCompletedWithinItsToleranceDelaysNothing() {
        StepInstance ship = planned(step("SHIP", Constants.STATUS_CREATED), LocalDateTime.of(2022, 5, 23, 1, 46));
        ship.setLateAfter(LocalDateTime.of(2022, 5, 23, 2, 1));
        StepInstance deliver = planned(step("DELIVER", Constants.STATUS_CREATED), LocalDateTime.of(2022, 5, 23, 9, 46));
        givenSteps(ship, deliver);
        givenDefinition("SHIP", List.of("ORDER_SHIPPED_EVENT"), List.of());
        givenDefinition("DELIVER", List.of("ORDER_DELIVERED_EVENT"), List.of());

        service.recordMilestone("ORDER_SHIPPED_EVENT", process, LocalDateTime.of(2022, 5, 23, 2, 0));

        assertThat(ship.getTimeliness()).isEqualTo(Timeliness.ON_TIME);
        assertThat(deliver.getExpectedAt()).isNull();
        assertThat(deliver.getTimeliness()).isNull();
    }

    @Test
    void completionPlansTheStepsThatCountFromIt() {
        StepInstance ship = step("SHIP", Constants.STATUS_CREATED);
        StepInstance deliver = step("DELIVER", Constants.STATUS_CREATED);
        givenSteps(ship, deliver);
        givenDefinition("SHIP", List.of("ORDER_SHIPPED_EVENT"), List.of());
        StepDefinition deliverDefinition = givenDefinition("DELIVER", List.of("ORDER_DELIVERED_EVENT"), List.of());
        deliverDefinition.setPlannedAfter("SHIP");
        deliverDefinition.setPlannedWithin("PT8H");
        deliverDefinition.setTolerance("PT15M");

        service.recordMilestone("ORDER_SHIPPED_EVENT", process, SHIPPED_AT);

        assertThat(deliver.getPlannedAt()).isEqualTo(LocalDateTime.of(2022, 5, 23, 9, 30));
        assertThat(deliver.getLateAfter()).isEqualTo(LocalDateTime.of(2022, 5, 23, 9, 45));
        verify(stepInstancePublisherService).publish(deliver, "PLANNED");
    }

    @Test
    void overdueStepPutsLaterStepsAtRisk() {
        StepInstance ship = planned(step("SHIP", Constants.STATUS_CREATED), LocalDateTime.of(2022, 5, 23, 1, 46));
        StepInstance deliver = planned(step("DELIVER", Constants.STATUS_CREATED), LocalDateTime.of(2022, 5, 23, 9, 46));
        when(stepInstanceService.getStepInstancesByProcessInstanceId(TENANT, process.getId())).thenReturn(List.of(ship, deliver));
        givenDefinition("SHIP", List.of("ORDER_SHIPPED_EVENT"), List.of());
        givenDefinition("DELIVER", List.of("ORDER_DELIVERED_EVENT"), List.of());

        when(processInstanceService.getProcessInstance(TENANT, process.getId())).thenReturn(process);

        service.markOverdue(ship, LocalDateTime.of(2022, 5, 23, 4, 0));

        assertThat(ship.getTimeliness()).isEqualTo(Timeliness.OVERDUE);
        verify(stepInstancePublisherService).publish(ship, "OVERDUE");
        assertThat(deliver.getTimeliness()).isEqualTo(Timeliness.AT_RISK);
        assertThat(deliver.getExpectedAt()).isEqualTo(LocalDateTime.of(2022, 5, 23, 12, 0));
        assertThat(registry.get("aktimetrix.steps.overdue").counter().count()).isEqualTo(1);
    }

    @Test
    void aStepAlreadyAtRiskKeepsItsLaterForecast() {
        StepInstance ship = planned(step("SHIP", Constants.STATUS_CREATED), LocalDateTime.of(2022, 5, 23, 1, 46));
        ship.setTimeliness(Timeliness.OVERDUE);
        StepInstance deliver = planned(step("DELIVER", Constants.STATUS_CREATED), LocalDateTime.of(2022, 5, 23, 9, 46));
        deliver.setTimeliness(Timeliness.AT_RISK);
        deliver.setExpectedAt(LocalDateTime.of(2022, 5, 23, 9, 47));
        givenSteps(ship, deliver);
        givenDefinition("SHIP", List.of("ORDER_SHIPPED_EVENT"), List.of());
        givenDefinition("DELIVER", List.of("ORDER_DELIVERED_EVENT"), List.of());

        service.recordMilestone("ORDER_SHIPPED_EVENT", process, LocalDateTime.of(2022, 5, 23, 3, 0));

        assertThat(deliver.getExpectedAt()).isEqualTo(LocalDateTime.of(2022, 5, 23, 11, 0));
        verify(stepInstanceService).save(deliver);
        verify(stepInstancePublisherService).publish(deliver, "AT_RISK");
        assertThat(registry.find("aktimetrix.steps.at.risk").counter()).isNull();
    }

    private static StepInstance planned(StepInstance step, LocalDateTime plannedAt) {
        step.setPlannedAt(plannedAt);
        step.setLateAfter(plannedAt);
        return step;
    }

    static AktimetrixMetrics metrics(MeterRegistry registry) {
        return new AktimetrixMetrics(new StaticListableBeanFactory(Map.of("registry", registry))
                .getBeanProvider(MeterRegistry.class));
    }

    private StepInstance step(String code, String status) {
        StepInstance step = new StepInstance(TENANT, code, process.getId(), null, null, "1.0.0", status,
                LocalDateTime.now());
        step.setId(UUID.randomUUID().toString());
        step.setSequence(nextSequence++);
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
        processDefinition.getSteps().add(definition);
        return definition;
    }
}
