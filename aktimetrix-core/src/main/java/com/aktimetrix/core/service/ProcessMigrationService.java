package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.api.PublishedEvents;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.exception.ProcessHandlerNotFoundException;
import com.aktimetrix.core.impl.DefaultContext;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import com.aktimetrix.core.store.AktimetrixTransactions;
import com.aktimetrix.core.store.StoreDocuments;
import com.aktimetrix.core.transferobjects.EventContext.Cause;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Moves the running instances of a process to the current revision of its definition, for example to apply a
 * corrected plan to orders already under way. Each instance is migrated in its own unit of work:
 * <ul>
 *     <li>steps added by the revision are created and planned, by their meters and durations;</li>
 *     <li>steps it removed are skipped while open, and kept once they happened;</li>
 *     <li>steps still awaited are planned again from the new durations and tolerances, from the process start or
 *     the actual time of the step they follow; an overdue step whose new deadline is still ahead is awaited again;</li>
 *     <li>the process's own deadline follows the new {@code plannedWithin} and {@code tolerance}.</li>
 * </ul>
 * What already happened is kept: completed steps, their actual times and measurements, and their timeliness.
 */
@Service
@RequiredArgsConstructor
public class ProcessMigrationService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessMigrationService.class);

    private final ProcessInstanceService processInstanceService;
    private final ProcessDefinitionService processDefinitionService;
    private final StepInstanceService stepInstanceService;
    private final StepInstancePublisherService stepInstancePublisherService;
    private final ProcessInstancePublisherService processInstancePublisherService;
    private final RegistryService registryService;
    private final AktimetrixTransactions transactions;
    private final Clock clock;

    /**
     * Migrates every running instance of the process that follows an older revision.
     *
     * @return what was migrated
     * @throws IllegalArgumentException when the tenant has no definition of the process
     */
    public Migration migrate(String tenant, String processCode) {
        final ProcessDefinition current = processDefinitionService.currentDefinition(tenant, processCode);
        if (current == null) {
            throw new IllegalArgumentException("Tenant " + tenant + " has no definition of the " + processCode + " process");
        }
        final Migration migration = new Migration(processCode, current.getRevision(), new ArrayList<>(), 0,
                new ArrayList<>());
        for (ProcessInstance listed : processInstanceService.getRunning(tenant, processCode)) {
            if (listed.getDefinition() != null && Objects.equals(listed.getDefinitionRevision(), current.getRevision())) {
                migration.setUpToDate(migration.getUpToDate() + 1);
                continue;
            }
            try {
                ProcessingContext.run(new Cause(Cause.MIGRATION, null, null), LocalDateTime.now(clock),
                        () -> transactions.run(() -> migrate(
                                processInstanceService.getProcessInstance(tenant, listed.getId()),
                                StoreDocuments.copy(current))));
                migration.getMigrated().add(listed.getId());
            } catch (RuntimeException e) {
                logger.error("Process instance {} could not be migrated to revision {} of {}; it keeps revision {}",
                        listed.getId(), current.getRevision(), processCode, listed.getDefinitionRevision(), e);
                migration.getFailed().add(listed.getId());
            }
        }
        logger.info("Migrated {} instances of {} to revision {}; {} up to date, {} failed",
                migration.getMigrated().size(), processCode, current.getRevision(), migration.getUpToDate(),
                migration.getFailed().size());
        return migration;
    }

    private void migrate(ProcessInstance process, ProcessDefinition definition) {
        if (process == null || process.isComplete()) {
            return;   // ended meanwhile
        }
        final LocalDateTime now = LocalDateTime.now(clock);
        logger.info("Migrating process instance {} from revision {} to {}", process.getId(),
                process.getDefinitionRevision(), definition.getRevision());
        process.setDefinition(definition);
        process.setDefinitionRevision(definition.getRevision());
        replanProcess(process, definition, now);

        final Map<String, StepInstance> existing = new LinkedHashMap<>();
        final List<StepInstance> steps = stepInstanceService.getStepInstancesByProcessInstanceId(process.getTenant(),
                process.getId());
        steps.forEach(step -> existing.put(step.getStepCode(), step));
        final Map<String, Object> metadata = steps.isEmpty() ? process.getMetadata() : steps.get(0).getMetadata();
        final List<StepInstance> ordered = new ArrayList<>();
        final List<StepInstance> created = new ArrayList<>();
        int sequence = 0;
        for (StepDefinition stepDefinition : definition.getSteps() == null ? List.<StepDefinition>of()
                : definition.getSteps()) {
            StepInstance step = existing.remove(stepDefinition.getStepCode());
            if (step == null) {
                step = new StepInstance(process.getTenant(), stepDefinition.getStepCode(), process.getId(),
                        stepDefinition.getGroupCode(), stepDefinition.getFunctionalCtxCode(), Constants.DEFAULT_VERSION,
                        Constants.STATUS_CREATED, now);
                step.setMetadata(metadata);
                created.add(step);
            }
            step.setSequence(sequence++);
            ordered.add(step);
        }
        for (StepInstance removed : existing.values()) {
            removed.setSequence(sequence++);
            if (stillToHappen(removed)) {
                removed.setStatus(Constants.STATUS_SKIPPED);
                stepInstanceService.save(removed);
                stepInstancePublisherService.publish(removed, PublishedEvents.Step.SKIPPED);
            } else {
                stepInstanceService.save(removed);
            }
        }
        for (StepInstance step : created) {
            stepInstanceService.save(step);
            stepInstancePublisherService.publish(step, PublishedEvents.Step.CREATED);
        }
        runMeters(process, definition, created);

        final Map<String, StepInstance> byCode = new LinkedHashMap<>();
        ordered.forEach(step -> byCode.put(step.getStepCode(), step));
        for (StepDefinition stepDefinition : definition.getSteps() == null ? List.<StepDefinition>of()
                : definition.getSteps()) {
            final StepInstance step = byCode.get(stepDefinition.getStepCode());
            final boolean replanned = stillToHappen(step) && replan(step, stepDefinition, process, byCode, now);
            stepInstanceService.save(step);
            if (replanned && !created.contains(step)) {
                stepInstancePublisherService.publish(step, PublishedEvents.Step.PLANNED);
            }
        }
        processInstanceService.saveProcessInstance(process);
        processInstancePublisherService.publish(process, PublishedEvents.Process.MIGRATED);
    }

    /**
     * Neither happened nor closed: open, overdue or not.
     */
    private static boolean stillToHappen(StepInstance step) {
        return step.getActualAt() == null && (Constants.STATUS_CREATED.equals(step.getStatus())
                || Constants.STATUS_STARTED.equals(step.getStatus()));
    }

    /**
     * The process's own deadline, from the new {@code plannedWithin} and {@code tolerance}.
     */
    private static void replanProcess(ProcessInstance process, ProcessDefinition definition, LocalDateTime now) {
        if (definition.plannedWithinDuration() != null && process.getStartedAt() != null) {
            process.setPlannedAt(process.getStartedAt().plus(definition.plannedWithinDuration()));
        }
        process.setLateAfter(process.getPlannedAt() == null ? null
                : process.getPlannedAt().plus(definition.toleranceDuration()));
        if (process.getTimeliness() == Timeliness.OVERDUE && process.getLateAfter() != null
                && process.getLateAfter().isAfter(now)) {
            process.setTimeliness(null);
        }
    }

    /**
     * Plans a step still awaited from its new definition.
     *
     * @return whether its plan changed
     */
    private static boolean replan(StepInstance step, StepDefinition definition, ProcessInstance process,
                                  Map<String, StepInstance> steps, LocalDateTime now) {
        final LocalDateTime plannedBefore = step.getPlannedAt();
        final LocalDateTime lateAfterBefore = step.getLateAfter();
        final Timeliness timelinessBefore = step.getTimeliness();
        if (definition.plannedWithinDuration() != null) {
            final LocalDateTime from;
            if (definition.getPlannedAfter() == null) {
                from = process.getStartedAt();
            } else {
                final StepInstance after = steps.get(definition.getPlannedAfter());
                from = after == null ? null : after.getActualAt();   // not happened yet: planned when it does
            }
            step.setPlannedAt(from == null ? null : from.plus(definition.plannedWithinDuration()));
        }
        step.setLateAfter(step.getPlannedAt() == null ? null : step.getPlannedAt().plus(definition.toleranceDuration()));
        if (step.getTimeliness() == Timeliness.OVERDUE) {
            if (step.getLateAfter() == null || step.getLateAfter().isAfter(now)) {
                step.setTimeliness(null);   // awaited again: its new deadline is ahead
            }
        } else if (step.getTimeliness() == Timeliness.AT_RISK) {
            final boolean stillAtRisk = step.getExpectedAt() != null && step.getLateAfter() != null
                    && step.getExpectedAt().isAfter(step.getLateAfter());
            if (!stillAtRisk) {
                step.setTimeliness(null);
            }
        }
        return !Objects.equals(plannedBefore, step.getPlannedAt()) || !Objects.equals(lateAfterBefore, step.getLateAfter())
                || timelinessBefore != step.getTimeliness();
    }

    /**
     * Plans the new steps by their meters, as when a process starts.
     */
    private void runMeters(ProcessInstance process, ProcessDefinition definition, List<StepInstance> created) {
        if (created.isEmpty()) {
            return;
        }
        final Processor meterProcessor;
        try {
            meterProcessor = registryService.getProcessHandler(Constants.METER_PROCESSOR);
        } catch (ProcessHandlerNotFoundException e) {
            return;
        }
        for (StepInstance step : created) {
            final DefaultContext context = new DefaultContext();
            context.setTenant(process.getTenant());
            context.setProcessType(Constants.METER_PROCESSOR);
            context.setProperty(Constants.PROCESS_DEFINITION, definition);
            context.setStepInstances(new ArrayList<>(List.of(step)));
            meterProcessor.process(context);
        }
    }

    /**
     * The result of a migration.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Migration {
        private String processCode;
        /**
         * The revision the instances were moved to.
         */
        private Long revision;
        /**
         * Ids of the instances migrated.
         */
        private List<String> migrated;
        /**
         * How many running instances already followed the revision.
         */
        private int upToDate;
        /**
         * Ids of the instances that could not be migrated, and keep their revision; see the log.
         */
        private List<String> failed;
    }
}
