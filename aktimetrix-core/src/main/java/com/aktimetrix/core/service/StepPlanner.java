package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Plans steps from the durations in their definitions, derives their deadlines, and forecasts the steps put at risk
 * when an earlier step runs late.
 * <ul>
 *     <li>A step with {@code plannedWithin} and no {@code plannedAfter} is planned from the process start.</li>
 *     <li>A step with {@code plannedAfter} is planned when that step completes, from its actual time.</li>
 *     <li>Every planned step, by duration or by meter, gets {@code lateAfter}: its planned time plus its
 *     {@code tolerance}.</li>
 * </ul>
 * The planner only changes the step instances it is given; callers save them.
 */
@Component
public class StepPlanner {
    private static final Logger logger = LoggerFactory.getLogger(StepPlanner.class);

    /**
     * Plans the steps of a newly started process and sets the deadline of every planned step.
     *
     * @return the steps whose plan changed
     */
    public List<StepInstance> planNewSteps(List<StepInstance> steps, Map<String, StepDefinition> definitions,
                                           LocalDateTime startedAt) {
        final List<StepInstance> changed = new ArrayList<>();
        for (StepInstance step : steps) {
            final StepDefinition definition = definitions.get(step.getStepCode());
            if (definition == null) {
                continue;
            }
            boolean planned = false;
            if (step.getPlannedAt() == null && definition.getPlannedAfter() == null
                    && definition.plannedWithinDuration() != null && startedAt != null) {
                step.setPlannedAt(startedAt.plus(definition.plannedWithinDuration()));
                planned = true;
            }
            if (setDeadline(step, definition) || planned) {
                changed.add(step);
            }
        }
        return changed;
    }

    /**
     * Plans the steps that are planned after the completed step, from its actual time.
     *
     * @return the steps planned
     */
    public List<StepInstance> planAfter(StepInstance completed, List<StepInstance> steps,
                                        Map<String, StepDefinition> definitions) {
        final List<StepInstance> planned = new ArrayList<>();
        for (StepInstance step : steps) {
            final StepDefinition definition = definitions.get(step.getStepCode());
            if (definition == null || !completed.getStepCode().equals(definition.getPlannedAfter())
                    || definition.plannedWithinDuration() == null
                    || isClosed(step) || step.getPlannedAt() != null) {
                continue;
            }
            step.setPlannedAt(completed.getActualAt().plus(definition.plannedWithinDuration()));
            setDeadline(step, definition);
            logger.debug("Planned {} at {}, {} after {}", step.getStepCode(), step.getPlannedAt(),
                    definition.getPlannedWithin(), completed.getStepCode());
            planned.add(step);
        }
        return planned;
    }

    /**
     * Forecasts the later steps of the process as delayed by as much as {@code source} is, and marks
     * {@link Timeliness#AT_RISK} those forecast past their deadline. Steps planned from {@code source}'s actual
     * time already include its delay and are left alone.
     *
     * @param delay how late {@code source} is against its plan
     * @return the steps newly put at risk
     */
    public List<StepInstance> forecast(StepInstance source, Duration delay, List<StepInstance> steps,
                                       Map<String, StepDefinition> definitions) {
        final List<StepInstance> atRisk = new ArrayList<>();
        if (delay == null || delay.isNegative() || delay.isZero()) {
            return atRisk;
        }
        for (StepInstance step : steps) {
            final StepDefinition definition = definitions.get(step.getStepCode());
            if (step.getSequence() <= source.getSequence() || step.getPlannedAt() == null
                    || isClosed(step)
                    || (definition != null && source.getStepCode().equals(definition.getPlannedAfter()))) {
                continue;
            }
            final LocalDateTime expected = step.getPlannedAt().plus(delay);
            if (step.getExpectedAt() == null || expected.isAfter(step.getExpectedAt())) {
                step.setExpectedAt(expected);
            }
            if (step.getTimeliness() == null && expected.isAfter(deadline(step))) {
                step.setTimeliness(Timeliness.AT_RISK);
                atRisk.add(step);
            }
        }
        return atRisk;
    }

    /**
     * {@link Timeliness#ON_TIME} or {@link Timeliness#LATE} for a step completed at {@code actualAt}, or
     * {@code null} when it had no plan.
     */
    public Timeliness judge(StepInstance step, LocalDateTime actualAt) {
        if (step.getPlannedAt() == null) {
            return null;
        }
        return actualAt.isAfter(deadline(step)) ? Timeliness.LATE : Timeliness.ON_TIME;
    }

    /**
     * Completed, or skipped as an alternative not taken: nothing left to plan or forecast.
     */
    private static boolean isClosed(StepInstance step) {
        return Constants.STATUS_COMPLETED.equals(step.getStatus()) || Constants.STATUS_SKIPPED.equals(step.getStatus())
                || step.getActualAt() != null;
    }

    private static LocalDateTime deadline(StepInstance step) {
        return step.getLateAfter() != null ? step.getLateAfter() : step.getPlannedAt();
    }

    private static boolean setDeadline(StepInstance step, StepDefinition definition) {
        if (step.getPlannedAt() == null) {
            return false;
        }
        final LocalDateTime lateAfter = step.getPlannedAt().plus(definition.toleranceDuration());
        if (lateAfter.equals(step.getLateAfter())) {
            return false;
        }
        step.setLateAfter(lateAfter);
        return true;
    }
}
