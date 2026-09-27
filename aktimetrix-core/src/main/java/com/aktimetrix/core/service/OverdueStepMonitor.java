package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.repository.StepInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detects events that never arrive: periodically marks steps whose planned time has passed without completing as
 * {@link Timeliness#OVERDUE}, and publishes an OVERDUE step event for each so consumers can alert on it.
 * <p>
 * Disable with {@code aktimetrix.monitor.enabled=false}; tune with {@code aktimetrix.monitor.overdue-check-interval}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aktimetrix.monitor", name = "enabled", matchIfMissing = true)
public class OverdueStepMonitor {
    private static final Logger logger = LoggerFactory.getLogger(OverdueStepMonitor.class);

    private final StepInstanceRepository stepInstanceRepository;
    private final StepInstanceService stepInstanceService;
    private final StepInstancePublisherService stepInstancePublisherService;
    private final Clock clock;

    /**
     * @return the steps marked overdue by this check
     */
    @Scheduled(fixedDelayString = "${aktimetrix.monitor.overdue-check-interval:PT1M}",
            initialDelayString = "${aktimetrix.monitor.overdue-check-interval:PT1M}")
    public List<StepInstance> checkOverdueSteps() {
        final List<StepInstance> overdue = stepInstanceRepository.findOverdue(LocalDateTime.now(clock));
        for (StepInstance step : overdue) {
            logger.warn("Step {} of process instance {} is overdue: planned at {}", step.getStepCode(),
                    step.getProcessInstanceId(), step.getPlannedAt());
            step.setTimeliness(Timeliness.OVERDUE);
            stepInstanceService.save(step);
            stepInstancePublisherService.publish(step, Timeliness.OVERDUE.name());
        }
        return overdue;
    }
}
