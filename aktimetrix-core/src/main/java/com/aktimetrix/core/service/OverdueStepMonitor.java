package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.repository.StepInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detects events that never arrive: periodically marks steps whose deadline (planned time plus tolerance) has passed
 * without completing as {@link Timeliness#OVERDUE}, publishes an OVERDUE step event for each so consumers can alert
 * on it, and forecasts the later steps of the process as at risk.
 * <p>
 * Disable with {@code aktimetrix.monitor.enabled=false}; tune with {@code aktimetrix.monitor.overdue-check-interval}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aktimetrix.monitor", name = "enabled", matchIfMissing = true)
public class OverdueStepMonitor {
    private final StepInstanceRepository stepInstanceRepository;
    private final StepProgressService stepProgressService;
    private final Clock clock;

    /**
     * @return the steps marked overdue by this check
     */
    @Scheduled(fixedDelayString = "${aktimetrix.monitor.overdue-check-interval:PT1M}",
            initialDelayString = "${aktimetrix.monitor.overdue-check-interval:PT1M}")
    public List<StepInstance> checkOverdueSteps() {
        final LocalDateTime now = LocalDateTime.now(clock);
        final List<StepInstance> overdue = stepInstanceRepository.findOverdue(now);
        overdue.forEach(step -> stepProgressService.markOverdue(step, now));
        return overdue;
    }
}
