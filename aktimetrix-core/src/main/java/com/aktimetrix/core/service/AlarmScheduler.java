package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.model.Alarm;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.store.AktimetrixTransactions;
import com.aktimetrix.core.store.AlarmStore;
import com.aktimetrix.core.store.ProcessInstanceStore;
import com.aktimetrix.core.store.StepInstanceStore;
import com.aktimetrix.core.transferobjects.EventContext.Cause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Fires the alarms set at the deadlines of steps and processes. When an alarm fires and its step or process is still
 * open, it is marked {@link Timeliness#OVERDUE}; when the event that completes it arrives later, it is compared with
 * its plan as usual and judged {@code LATE}.
 * <p>
 * Every application instance fires alarms. Each claims due alarms in batches, with a lease, so an alarm is fired by
 * one instance only, and one whose instance stopped mid-way is fired by another once its lease expires. Each alarm is
 * handled in its own unit of work.
 */
@Component
@ConditionalOnProperty(prefix = "aktimetrix.alarms", name = "enabled", matchIfMissing = true)
public class AlarmScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AlarmScheduler.class);
    private static final Set<String> CLOSED_STEP = Set.of(Constants.STATUS_COMPLETED, Constants.STATUS_CANCELLED,
            Constants.STATUS_SKIPPED);

    private final AlarmStore alarms;
    private final StepInstanceStore steps;
    private final ProcessInstanceStore processes;
    private final StepProgressService stepProgressService;
    private final AktimetrixTransactions transactions;
    private final AktimetrixProperties properties;
    private final AktimetrixMetrics metrics;
    private final Clock clock;

    public AlarmScheduler(AlarmStore alarms, StepInstanceStore steps, ProcessInstanceStore processes,
                          StepProgressService stepProgressService, AktimetrixTransactions transactions,
                          AktimetrixProperties properties, AktimetrixMetrics metrics, Clock clock) {
        this.alarms = alarms;
        this.steps = steps;
        this.processes = processes;
        this.stepProgressService = stepProgressService;
        this.transactions = transactions;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
        metrics.alarmsPending(alarms::countPending);
    }

    /**
     * Fires every alarm that is due.
     *
     * @return how many steps and processes were marked overdue
     */
    @Scheduled(fixedDelayString = "${aktimetrix.alarms.check-interval:PT5S}",
            initialDelayString = "${aktimetrix.alarms.check-interval:PT5S}")
    public int fireDueAlarms() {
        final int batch = properties.getAlarms().getBatchSize();
        int overdue = 0;
        List<Alarm> due;
        do {
            final LocalDateTime now = LocalDateTime.now(clock);
            final Instant claimedAt = clock.instant();
            due = alarms.claimDue(now, claimedAt, claimedAt.plus(properties.getAlarms().getLease()), batch);
            for (Alarm alarm : due) {
                if (fire(alarm, now)) {
                    overdue++;
                }
            }
        } while (due.size() == batch);
        return overdue;
    }

    private boolean fire(Alarm alarm, LocalDateTime now) {
        final boolean[] marked = {false};
        try {
            ProcessingContext.run(new Cause(Cause.DEADLINE, null, null), alarm.getDueAt(), () -> transactions.run(() ->
                    marked[0] = Alarm.STEP.equals(alarm.getKind()) ? fireStep(alarm, now) : fireProcess(alarm, now)));
            if (marked[0]) {
                metrics.alarmFired(alarm, Duration.between(alarm.getDueAt(), now));
            }
        } catch (OptimisticLockingFailureException e) {
            // its event arrived meanwhile: the alarm is fired again after its lease, and then finds it settled
            logger.debug("Alarm {} lost a race with an event; retried after its lease", alarm.getId());
        } catch (RuntimeException e) {
            logger.error("Could not fire alarm {}; retried after its lease", alarm.getId(), e);
        }
        return marked[0];
    }

    private boolean fireStep(Alarm alarm, LocalDateTime now) {
        final StepInstance step = steps.findById(alarm.getTargetId()).orElse(null);
        if (step == null || CLOSED_STEP.contains(step.getStatus()) || step.getTimeliness() == Timeliness.OVERDUE
                || step.getLateAfter() == null) {
            alarms.cancel(alarm.getId());
            return false;
        }
        if (step.getLateAfter().isAfter(now)) {
            alarms.schedule(Alarm.of(Alarm.STEP, step.getTenant(), step.getId(), step.getProcessInstanceId(),
                    step.getLateAfter()));
            return false;
        }
        stepProgressService.markOverdue(step, now);
        return true;
    }

    private boolean fireProcess(Alarm alarm, LocalDateTime now) {
        final ProcessInstance process = processes.findById(alarm.getTenant(), alarm.getTargetId()).orElse(null);
        if (process == null || process.isComplete() || process.getTimeliness() == Timeliness.OVERDUE
                || process.getLateAfter() == null) {
            alarms.cancel(alarm.getId());
            return false;
        }
        if (process.getLateAfter().isAfter(now)) {
            alarms.schedule(Alarm.of(Alarm.PROCESS, process.getTenant(), process.getId(), process.getId(),
                    process.getLateAfter()));
            return false;
        }
        stepProgressService.markProcessOverdue(process);
        return true;
    }
}
