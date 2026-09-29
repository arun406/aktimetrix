package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.repository.StepInstanceRepository;
import com.aktimetrix.core.storage.AktimetrixTransactions;
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Detects events that never arrive: periodically marks steps whose deadline (planned time plus tolerance) has passed
 * without completing as {@link Timeliness#OVERDUE}, publishes an OVERDUE step event for each so consumers can alert
 * on it, and forecasts the later steps of the process as at risk.
 * <p>
 * Safe to run on every application instance: a step is saved with a version check before its OVERDUE event is
 * queued, so when two instances find the same step, only the first marks and publishes it.
 * <p>
 * Disable with {@code aktimetrix.monitor.enabled=false}; tune with {@code aktimetrix.monitor.overdue-check-interval}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aktimetrix.monitor", name = "enabled", matchIfMissing = true)
public class OverdueStepMonitor {
    private static final Logger logger = LoggerFactory.getLogger(OverdueStepMonitor.class);

    private final StepInstanceRepository stepInstanceRepository;
    private final StepProgressService stepProgressService;
    private final Clock clock;
    private final AktimetrixTransactions transactions;

    /**
     * @return the steps marked overdue by this check
     */
    @Scheduled(fixedDelayString = "${aktimetrix.monitor.overdue-check-interval:PT1M}",
            initialDelayString = "${aktimetrix.monitor.overdue-check-interval:PT1M}")
    public List<StepInstance> checkOverdueSteps() {
        final LocalDateTime now = LocalDateTime.now(clock);
        final List<StepInstance> overdue = new ArrayList<>();
        for (StepInstance found : stepInstanceRepository.findOverdue(now)) {
            try {
                // read the step again: marking an earlier step overdue may have changed it, e.g. put it at risk
                final AtomicReference<StepInstance> marked = new AtomicReference<>();
                transactions.run(() -> stepInstanceRepository.findById(found.getId().toHexString())
                        .filter(current -> isStillOverdue(current, now))
                        .ifPresent(current -> {
                            stepProgressService.markOverdue(current, now);
                            marked.set(current);
                        }));
                if (marked.get() != null) {
                    overdue.add(marked.get());
                }
            } catch (OptimisticLockingFailureException e) {
                // completed, or marked by another instance, since it was read; the next check sees its new state
                logger.debug("Step {} changed while being marked overdue; skipped", found.getId());
            } catch (RuntimeException e) {
                logger.error("Could not mark step {} overdue; retrying on the next check", found.getId(), e);
            }
        }
        return overdue;
    }

    private static boolean isStillOverdue(StepInstance step, LocalDateTime now) {
        return !Constants.STATUS_COMPLETED.equals(step.getStatus()) && !Constants.STATUS_CANCELLED.equals(step.getStatus())
                && !Constants.STATUS_SKIPPED.equals(step.getStatus()) && step.getTimeliness() != Timeliness.OVERDUE
                && step.getLateAfter() != null && step.getLateAfter().isBefore(now);
    }
}
