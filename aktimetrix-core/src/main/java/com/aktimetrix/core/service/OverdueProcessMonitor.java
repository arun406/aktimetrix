package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.store.AktimetrixTransactions;
import com.aktimetrix.core.store.ProcessInstanceStore;
import com.aktimetrix.core.transferobjects.EventContext.Cause;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Periodically marks running processes whose own deadline (the process definition's {@code plannedWithin} plus
 * tolerance) has passed as {@link Timeliness#OVERDUE}, and publishes an OVERDUE process event for each. Like
 * {@link OverdueStepMonitor}, it is safe to run on every application instance.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aktimetrix.monitor", name = "enabled", matchIfMissing = true)
public class OverdueProcessMonitor {
    private static final Logger logger = LoggerFactory.getLogger(OverdueProcessMonitor.class);

    private final ProcessInstanceStore processInstanceStore;
    private final StepProgressService stepProgressService;
    private final Clock clock;
    private final AktimetrixTransactions transactions;
    private static final Cause DEADLINE = new Cause(Cause.DEADLINE, null, null);

    /**
     * @return the processes marked overdue by this check
     */
    @Scheduled(fixedDelayString = "${aktimetrix.monitor.overdue-check-interval:PT10M}",
            initialDelayString = "${aktimetrix.monitor.overdue-check-interval:PT10M}")
    public List<ProcessInstance> checkOverdueProcesses() {
        final LocalDateTime now = LocalDateTime.now(clock);
        final List<ProcessInstance> overdue = new ArrayList<>();
        for (ProcessInstance process : processInstanceStore.findOverdue(now)) {
            try {
                ProcessingContext.run(DEADLINE, now,
                        () -> transactions.run(() -> stepProgressService.markProcessOverdue(process)));
                overdue.add(process);
            } catch (OptimisticLockingFailureException e) {
                logger.debug("Process {} changed while being marked overdue; skipped", process.getId());
            } catch (RuntimeException e) {
                logger.error("Could not mark process {} overdue; retrying on the next check", process.getId(), e);
            }
        }
        return overdue;
    }
}
