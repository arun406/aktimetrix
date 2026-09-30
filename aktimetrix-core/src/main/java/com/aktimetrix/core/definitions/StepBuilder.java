package com.aktimetrix.core.definitions;

import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.StepDefinition;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Builds a step: the events that start and complete it, its plan and its measurements. Within a process, it adapts
 * the tenant's shared step of the same code, if there is one: only what is set here replaces it.
 */
public final class StepBuilder extends MeasurementsBuilder<StepBuilder> {

    private final StepDefinition definition = new StepDefinition();
    private final String tenant;
    private final String processCode;
    private final List<Definitions.Rule> rules;

    StepBuilder(String stepCode, String tenant, String processCode, List<Definitions.Rule> rules) {
        definition.setStepCode(stepCode);
        this.tenant = tenant;
        this.processCode = processCode;
        this.rules = rules;
    }

    public StepBuilder name(String name) {
        definition.setStepName(name);
        return this;
    }

    /**
     * The events of a milestone: the step starts and completes on the first of them.
     */
    public StepBuilder on(String... eventCodes) {
        definition.setStartEventCodes(Arrays.asList(eventCodes));
        return this;
    }

    /**
     * The events that start a step that runs until one of its {@link #endsOn} events.
     */
    public StepBuilder startsOn(String... eventCodes) {
        return on(eventCodes);
    }

    /**
     * The events that complete a step started by one of its {@link #startsOn} events.
     */
    public StepBuilder endsOn(String... eventCodes) {
        definition.setEndEventCodes(Arrays.asList(eventCodes));
        return this;
    }

    /**
     * The events that report progress while the step runs: each records interim readings of its actual
     * measurements, compared with the plan.
     */
    public StepBuilder progressOn(String... eventCodes) {
        definition.setProgressEventCodes(Arrays.asList(eventCodes));
        return this;
    }

    /**
     * The step may not happen: the process can complete without it.
     */
    public StepBuilder optional() {
        definition.setOptionalInd("Y");
        return this;
    }

    /**
     * Plans the step after another step completes; without it, after the process starts. See {@link #within}.
     */
    public StepBuilder after(String stepCode) {
        definition.setPlannedAfter(stepCode);
        return this;
    }

    /**
     * The step is planned this long after the step it follows, or after the process starts, e.g. {@code "PT2H"}.
     */
    public StepBuilder within(String isoDuration) {
        definition.setPlannedWithin(iso(isoDuration));
        return this;
    }

    public StepBuilder within(Duration duration) {
        definition.setPlannedWithin(duration.toString());
        return this;
    }

    /**
     * How long the step may run past its planned time before it counts as late or overdue, e.g. {@code "PT15M"}.
     */
    public StepBuilder tolerance(String isoDuration) {
        definition.setTolerance(iso(isoDuration));
        return this;
    }

    public StepBuilder tolerance(Duration duration) {
        definition.setTolerance(duration.toString());
        return this;
    }

    /**
     * Plans the step's time by a rule, e.g. {@code planTime(step -> metadataTime(step, "createdAt").plusHours(4))}.
     * It replaces a plan by duration.
     */
    public StepBuilder planTime(Function<StepInstance, LocalDateTime> rule) {
        return plan("TIME", "TIMESTAMP", rule::apply);
    }

    /**
     * Plans a measurement of the step by a rule, computed when the step is created. The rule applies to this step of
     * this process only; for a step shared by the tenant's processes, to every process that has it and no rule of its
     * own.
     *
     * @param rule returns the planned value; {@code String.valueOf} of it is kept
     */
    public StepBuilder plan(String measurementCode, String unit, Function<StepInstance, Object> rule) {
        planned(measurementCode);
        rules.add(new Definitions.Rule(tenant, measurementCode, unit, definition.getStepCode(), rule, processCode, null));
        return this;
    }

    StepDefinition build() {
        if (!measurements.isEmpty()) {
            definition.setMeasurements(measurements);
        }
        return definition;
    }
}
