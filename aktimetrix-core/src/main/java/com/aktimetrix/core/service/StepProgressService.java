package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.impl.DefaultContext;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.service.StepDefinitionService;
import com.aktimetrix.core.transferobjects.Event;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records what actually happens to a process instance: moves its steps through their lifecycle when a business
 * event matches the step definition's start or end event codes, captures an actual TIME measurement when a step
 * completes, and judges the step against its plan.
 * <p>
 * A step whose definition has no end event codes is a single milestone and completes on its start event.
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
    private final Clock clock;

    /**
     * Applies the event to every active process instance of the business entity.
     *
     * @return actual measurements recorded for the steps this event completed
     */
    public List<MeasurementInstance> recordMilestones(String tenant, String entityType, String entityId,
                                                      String eventCode, LocalDateTime occurredAt) {
        final List<MeasurementInstance> actuals = new ArrayList<>();
        final List<ProcessInstance> processInstances =
                processInstanceService.getActiveProcessInstances(tenant, entityType, entityId);
        if (processInstances.isEmpty()) {
            logger.debug("No active process instance for {} {}; {} records nothing", entityType, entityId, eventCode);
        }
        processInstances.forEach(processInstance ->
                actuals.addAll(recordMilestone(eventCode, processInstance, occurredAt)));
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
        final String tenant = processInstance.getTenant();
        final List<StepInstance> steps = stepInstanceService.getStepInstancesByProcessInstanceId(tenant, processInstance.getId());
        final Map<String, StepDefinition> definitions = new HashMap<>();
        final List<MeasurementInstance> actuals = new ArrayList<>();

        for (StepInstance step : steps) {
            final StepDefinition definition = definitions.computeIfAbsent(step.getStepCode(),
                    code -> stepDefinitionService.findByStepCode(tenant, code));
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
                step.setTimeliness(timeliness(step.getPlannedAt(), occurredAt));
                actuals.add(actualTime(step, occurredAt));
            }
            stepInstanceService.save(step);
            stepInstancePublisherService.publish(step, nextStatus.toUpperCase());
        }

        if (!actuals.isEmpty()) {
            measurementInstanceService.saveMeasurementInstances(actuals);
            DefaultContext context = new DefaultContext();
            context.setTenant(tenant);
            context.setMeasurementInstances(actuals);
            measurementInstancePublisherService.postProcess(context);
            completeProcessIfDone(processInstance, steps, definitions);
        }
        return actuals;
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

    static Timeliness timeliness(LocalDateTime plannedAt, LocalDateTime actualAt) {
        if (plannedAt == null) {
            return null;
        }
        return actualAt.isAfter(plannedAt) ? Timeliness.LATE : Timeliness.ON_TIME;
    }

    private MeasurementInstance actualTime(StepInstance step, LocalDateTime occurredAt) {
        return new MeasurementInstance(step.getTenant(), Constants.MEASUREMENT_CODE_TIME, String.valueOf(occurredAt),
                Constants.MEASUREMENT_UNIT_TIMESTAMP, step.getProcessInstanceId(), step.getId(), step.getStepCode(),
                Constants.ACTUAL_MEASUREMENT_TYPE, step.getLocationCode(), ZonedDateTime.now(clock));
    }

    private void completeProcessIfDone(ProcessInstance processInstance, List<StepInstance> steps,
                                       Map<String, StepDefinition> definitions) {
        final boolean done = steps.stream()
                .filter(step -> !isOptional(definitions.get(step.getStepCode())))
                .allMatch(step -> Constants.STATUS_COMPLETED.equals(step.getStatus()));
        if (done && !processInstance.isComplete()) {
            logger.info("Process instance {} is complete", processInstance.getId());
            processInstance.setComplete(true);
            processInstance.setStatus(Constants.STATUS_COMPLETED);
            processInstanceService.saveProcessInstance(processInstance);
        }
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
