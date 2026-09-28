package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Conformance;
import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.impl.DefaultContext;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import com.aktimetrix.core.referencedata.service.StepDefinitionService;
import com.aktimetrix.core.transferobjects.Event;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Records what actually happens to a process instance: moves its steps through their lifecycle when a business
 * event matches the step definition's start or end event codes, captures an actual TIME measurement when a step
 * completes, and judges the step against its plan.
 * <p>
 * A step whose definition has no end event codes is a single milestone and completes on its start event. An event in
 * the process definition's cancel event codes cancels the process instead. When all mandatory steps have completed,
 * the process completes, and is judged against its own deadline if it has one.
 *
 * @author arun kumar kandakatla
 */
@Service
@RequiredArgsConstructor
public class StepProgressService {
    private static final Logger logger = LoggerFactory.getLogger(StepProgressService.class);

    private final StepInstanceService stepInstanceService;
    private final StepDefinitionService stepDefinitionService;
    private final ProcessInstanceService processInstanceService;
    private final MeasurementInstanceService measurementInstanceService;
    private final MeasurementInstancePublisherService measurementInstancePublisherService;
    private final StepInstancePublisherService stepInstancePublisherService;
    private final StepPlanner stepPlanner;
    private final AktimetrixMetrics metrics;
    private final Clock clock;
    private final ProcessDefinitionService processDefinitionService;
    private final ProcessInstancePublisherService processInstancePublisherService;
    private final ActualMeasurementService actualMeasurementService;

    /**
     * Applies the event to the business entity's process instances that are not cancelled: running ones, and completed
     * ones, whose optional steps may still happen, such as a rating after delivery.
     *
     * @return actual measurements recorded for the steps this event completed
     */
    public List<MeasurementInstance> recordMilestones(String tenant, String entityType, String entityId,
                                                      String eventCode, LocalDateTime occurredAt) {
        return recordMilestones(tenant, entityType, entityId, eventCode, occurredAt, null);
    }

    /**
     * Applies the event to the business entity's process instances that are not cancelled.
     *
     * @param event the event itself, from which actual measurements are read; may be {@code null}
     * @return actual measurements recorded for the steps and processes this event completed
     */
    public List<MeasurementInstance> recordMilestones(String tenant, String entityType, String entityId,
                                                      String eventCode, LocalDateTime occurredAt, Event<?, ?> event) {
        final List<MeasurementInstance> actuals = new ArrayList<>();
        final List<ProcessInstance> processInstances =
                processInstanceService.getNotCancelledProcessInstances(tenant, entityType, entityId);
        if (processInstances.isEmpty()) {
            logger.debug("No process instance for {} {}; {} records nothing", entityType, entityId, eventCode);
        }
        for (ProcessInstance processInstance : processInstances) {
            final ProcessDefinition definition = processDefinitionService.findByCode(tenant,
                    processInstance.getProcessCode());
            if (definition != null && contains(definition.getCancelEventCodes(), eventCode)) {
                if (processInstance.isComplete()) {
                    continue;   // too late to cancel
                }
                cancel(processInstance, occurredAt);
            } else {
                actuals.addAll(recordMilestone(eventCode, processInstance, occurredAt, event));
                if (definition != null && contains(definition.getEndEventCodes(), eventCode)
                        && !processInstance.isComplete()) {
                    actuals.addAll(end(processInstance, definition, occurredAt, event));
                }
            }
        }
        return actuals;
    }

    /**
     * Applies the event to the steps of the process instance, then saves and publishes the actual measurements.
     *
     * @param eventCode       code of the business event
     * @param processInstance process instance of the business entity the event is about
     * @param occurredAt      when the event happened in the business
     * @return actual measurements recorded for the steps this event completed
     */
    public List<MeasurementInstance> recordMilestone(String eventCode, ProcessInstance processInstance,
                                                     LocalDateTime occurredAt) {
        return recordMilestone(eventCode, processInstance, occurredAt, null);
    }

    /**
     * Applies the event to the steps of the process instance, then saves and publishes the actual measurements.
     *
     * @param event the event itself, from which actual measurements are read; may be {@code null}
     */
    public List<MeasurementInstance> recordMilestone(String eventCode, ProcessInstance processInstance,
                                                     LocalDateTime occurredAt, Event<?, ?> event) {
        final String tenant = processInstance.getTenant();
        final List<StepInstance> steps = stepInstanceService.getStepInstancesByProcessInstanceId(tenant, processInstance.getId());
        final Map<String, StepDefinition> definitions = definitions(tenant, processInstance.getProcessCode(), steps);
        final List<MeasurementInstance> actuals = new ArrayList<>();
        final List<StepInstance> completed = new ArrayList<>();

        for (StepInstance step : steps) {
            final StepDefinition definition = definitions.get(step.getStepCode());
            if (definition == null) {
                logger.warn("step definition not found for {} step", step.getStepCode());
                continue;
            }
            final String nextStatus = nextStatus(definition, step.getStatus(), eventCode);
            if (nextStatus == null) {
                continue;
            }
            logger.info("Step {} of process instance {}: {} -> {}", step.getStepCode(), processInstance.getId(),
                    step.getStatus(), nextStatus);
            step.setStatus(nextStatus);
            if (Constants.STATUS_COMPLETED.equals(nextStatus)) {
                step.setActualAt(occurredAt);
                step.setTimeliness(stepPlanner.judge(step, occurredAt));
                actuals.add(actualTime(step, occurredAt));
                actuals.addAll(actualMeasurementService.forStep(step, definition.getMeasurements(), event));
                completed.add(step);
                metrics.stepCompleted(step);
            }
            stepInstanceService.save(step);
            stepInstancePublisherService.publish(step, nextStatus.toUpperCase());
        }

        for (StepInstance step : completed) {
            stepPlanner.planAfter(step, steps, definitions).forEach(planned -> {
                stepInstanceService.save(planned);
                stepInstancePublisherService.publish(planned, "PLANNED");
            });
            forecast(step, step.getPlannedAt() == null ? null : Duration.between(step.getPlannedAt(), occurredAt),
                    steps, definitions);
        }

        if (!actuals.isEmpty()) {
            actuals.addAll(completeProcessIfDone(processInstance, steps, definitions, occurredAt, event));
            measurementInstanceService.saveMeasurementInstances(actuals);
            DefaultContext context = new DefaultContext();
            context.setTenant(tenant);
            context.setMeasurementInstances(actuals);
            measurementInstancePublisherService.postProcess(context);
        }
        return actuals;
    }

    /**
     * Marks a step whose deadline has passed without its event as {@link Timeliness#OVERDUE}, and forecasts the
     * later steps of its process as delayed by at least as much.
     */
    public void markOverdue(StepInstance step, LocalDateTime now) {
        logger.warn("Step {} of process instance {} is overdue: planned at {}", step.getStepCode(),
                step.getProcessInstanceId(), step.getPlannedAt());
        step.setTimeliness(Timeliness.OVERDUE);
        stepInstanceService.save(step);
        stepInstancePublisherService.publish(step, Timeliness.OVERDUE.name());
        metrics.stepOverdue(step);

        final List<StepInstance> steps = stepInstanceService.getStepInstancesByProcessInstanceId(step.getTenant(),
                step.getProcessInstanceId());
        final ProcessInstance processInstance = processInstanceService.getProcessInstance(step.getTenant(),
                step.getProcessInstanceId());
        final String processCode = processInstance == null ? null : processInstance.getProcessCode();
        forecast(step, Duration.between(step.getPlannedAt(), now), steps, definitions(step.getTenant(), processCode, steps));
    }

    private void forecast(StepInstance source, Duration delay, List<StepInstance> steps,
                          Map<String, StepDefinition> definitions) {
        stepPlanner.forecast(source, delay, steps, definitions).forEach(atRisk -> {
            logger.warn("Step {} of process instance {} is at risk: expected at {}, after its deadline {}",
                    atRisk.getStepCode(), atRisk.getProcessInstanceId(), atRisk.getExpectedAt(), atRisk.getLateAfter());
            stepInstanceService.save(atRisk);
            stepInstancePublisherService.publish(atRisk, Timeliness.AT_RISK.name());
            metrics.stepAtRisk(atRisk);
        });
    }

    /**
     * The definitions of the steps as their process uses them, by step code.
     */
    private Map<String, StepDefinition> definitions(String tenant, String processCode, List<StepInstance> steps) {
        final Map<String, StepDefinition> definitions = new HashMap<>();
        for (StepInstance step : steps) {
            definitions.computeIfAbsent(step.getStepCode(),
                    code -> stepDefinitionService.findStepDefinition(tenant, processCode, code));
        }
        definitions.values().removeIf(Objects::isNull);
        return definitions;
    }

    /**
     * When the event happened in the business, in the configured time zone: {@code eventTime} if present, else
     * {@code eventUTCTime}, else the time it is processed.
     */
    public LocalDateTime occurredAt(Event<?, ?> event) {
        if (event.getEventTime() != null) {
            return event.getEventTime().withZoneSameInstant(clock.getZone()).toLocalDateTime();
        }
        if (event.getEventUTCTime() != null) {
            return event.getEventUTCTime().atZone(ZoneOffset.UTC).withZoneSameInstant(clock.getZone()).toLocalDateTime();
        }
        return LocalDateTime.now(clock);
    }

    /**
     * Returns the status the step moves to on this event, or {@code null} when the event does not advance it.
     */
    String nextStatus(StepDefinition definition, String currentStatus, String eventCode) {
        if (Constants.STATUS_COMPLETED.equals(currentStatus)) {
            return null;
        }
        if (contains(definition.getEndEventCodes(), eventCode)) {
            return Constants.STATUS_COMPLETED;
        }
        if (contains(definition.getStartEventCodes(), eventCode)) {
            if (isEmpty(definition.getEndEventCodes())) {
                return Constants.STATUS_COMPLETED;
            }
            return Constants.STATUS_STARTED.equals(currentStatus) ? null : Constants.STATUS_STARTED;
        }
        return null;
    }

    /**
     * The step's actual TIME, compared with its planned time like any other measurement: the deviation is a
     * duration, and the conformance follows from its timeliness.
     */
    private MeasurementInstance actualTime(StepInstance step, LocalDateTime occurredAt) {
        final MeasurementInstance actual = new MeasurementInstance(step.getTenant(), Constants.MEASUREMENT_CODE_TIME,
                String.valueOf(occurredAt), Constants.MEASUREMENT_UNIT_TIMESTAMP, step.getProcessInstanceId(),
                step.getId(), step.getStepCode(), Constants.ACTUAL_MEASUREMENT_TYPE, step.getLocationCode(),
                ZonedDateTime.now(clock));
        if (step.getPlannedAt() != null) {
            actual.setPlannedValue(String.valueOf(step.getPlannedAt()));
            actual.setDeviation(Duration.between(step.getPlannedAt(), occurredAt).toString());
            actual.setConformance(step.getTimeliness() == Timeliness.ON_TIME
                    ? Conformance.WITHIN_TOLERANCE : Conformance.OUT_OF_TOLERANCE);
        }
        metrics.measurementRecorded(actual);
        return actual;
    }

    /**
     * Cancels the process instance: it and its open steps become {@code Cancelled}, so they are no longer monitored,
     * and a CANCELLED event is published for each.
     */
    public void cancel(ProcessInstance processInstance, LocalDateTime occurredAt) {
        logger.info("Process instance {} is cancelled", processInstance.getId());
        processInstance.setStatus(Constants.STATUS_CANCELLED);
        processInstance.setComplete(true);
        processInstance.setEndedAt(occurredAt);
        processInstanceService.saveProcessInstance(processInstance);
        processInstancePublisherService.publish(processInstance, "CANCELLED");
        metrics.processCancelled(processInstance);
        for (StepInstance step : stepInstanceService.getStepInstancesByProcessInstanceId(processInstance.getTenant(),
                processInstance.getId())) {
            if (!Constants.STATUS_COMPLETED.equals(step.getStatus())) {
                step.setStatus(Constants.STATUS_CANCELLED);
                stepInstanceService.save(step);
                stepInstancePublisherService.publish(step, "CANCELLED");
            }
        }
    }

    /**
     * Marks a running process whose deadline has passed as {@link Timeliness#OVERDUE}, and publishes it.
     */
    public void markProcessOverdue(ProcessInstance processInstance) {
        logger.warn("Process instance {} is overdue: planned to complete at {}", processInstance.getId(),
                processInstance.getPlannedAt());
        processInstance.setTimeliness(Timeliness.OVERDUE);
        processInstanceService.saveProcessInstance(processInstance);
        processInstancePublisherService.publish(processInstance, Timeliness.OVERDUE.name());
        metrics.processOverdue(processInstance);
    }

    /**
     * Implicit end: completes the process when its last mandatory step has completed, unless its definition declares
     * explicit end events.
     *
     * @return the actual measurements of the process, if this completed it
     */
    private List<MeasurementInstance> completeProcessIfDone(ProcessInstance processInstance, List<StepInstance> steps,
                                                            Map<String, StepDefinition> definitions,
                                                            LocalDateTime occurredAt, Event<?, ?> event) {
        if (processInstance.isComplete()) {
            return List.of();
        }
        final ProcessDefinition definition = processDefinitionService.findByCode(processInstance.getTenant(),
                processInstance.getProcessCode());
        if (definition != null && !isEmpty(definition.getEndEventCodes())) {
            return List.of();
        }
        final boolean done = steps.stream()
                .filter(step -> !isOptional(definitions.get(step.getStepCode())))
                .allMatch(step -> Constants.STATUS_COMPLETED.equals(step.getStatus()));
        return done ? complete(processInstance, definition, occurredAt, event) : List.of();
    }

    /**
     * Explicit end: an end event of the process arrived. Its mandatory steps still open become {@code Skipped};
     * optional ones stay open, as they may still happen.
     *
     * @return the actual measurements of the process
     */
    private List<MeasurementInstance> end(ProcessInstance processInstance, ProcessDefinition definition,
                                          LocalDateTime occurredAt, Event<?, ?> event) {
        final List<StepInstance> steps = stepInstanceService.getStepInstancesByProcessInstanceId(
                processInstance.getTenant(), processInstance.getId());
        final Map<String, StepDefinition> definitions = definitions(processInstance.getTenant(),
                processInstance.getProcessCode(), steps);
        for (StepInstance step : steps) {
            final boolean open = Constants.STATUS_CREATED.equals(step.getStatus())
                    || Constants.STATUS_STARTED.equals(step.getStatus());
            if (open && !isOptional(definitions.get(step.getStepCode()))) {
                step.setStatus(Constants.STATUS_SKIPPED);
                stepInstanceService.save(step);
                stepInstancePublisherService.publish(step, "SKIPPED");
            }
        }
        final List<MeasurementInstance> actuals = complete(processInstance, definition, occurredAt, event);
        if (!actuals.isEmpty()) {
            measurementInstanceService.saveMeasurementInstances(actuals);
            final DefaultContext context = new DefaultContext();
            context.setTenant(processInstance.getTenant());
            context.setMeasurementInstances(actuals);
            measurementInstancePublisherService.postProcess(context);
        }
        return actuals;
    }

    /**
     * Completes the process, judges it against its deadline, and records its actual measurements.
     */
    private List<MeasurementInstance> complete(ProcessInstance processInstance, ProcessDefinition definition,
                                               LocalDateTime occurredAt, Event<?, ?> event) {
        logger.info("Process instance {} is complete", processInstance.getId());
        processInstance.setComplete(true);
        processInstance.setStatus(Constants.STATUS_COMPLETED);
        processInstance.setEndedAt(occurredAt);
        if (processInstance.getLateAfter() != null) {
            processInstance.setTimeliness(occurredAt.isAfter(processInstance.getLateAfter())
                    ? Timeliness.LATE : Timeliness.ON_TIME);
        }
        processInstanceService.saveProcessInstance(processInstance);
        processInstancePublisherService.publish(processInstance, "COMPLETED");
        metrics.processCompleted(processInstance);
        return definition == null ? List.of()
                : actualMeasurementService.forProcess(processInstance, definition.getMeasurements(), event);
    }

    private static boolean isOptional(StepDefinition definition) {
        return definition != null && "Y".equalsIgnoreCase(definition.getOptionalInd());
    }

    private static boolean contains(List<String> codes, String code) {
        return codes != null && codes.contains(code);
    }

    private static boolean isEmpty(List<String> codes) {
        return codes == null || codes.isEmpty();
    }
}
