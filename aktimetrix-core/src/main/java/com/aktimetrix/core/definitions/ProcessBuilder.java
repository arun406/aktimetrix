package com.aktimetrix.core.definitions;

import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.referencedata.model.MeasurementDefinition;
import com.aktimetrix.core.referencedata.model.MetricDefinition;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builds a process: the entity it follows, the events that start, end and cancel it, its steps in order, its own
 * deadline, measurements and metrics.
 */
public final class ProcessBuilder extends MeasurementsBuilder<ProcessBuilder> {

    static final String CONFIRMED = "CONFIRMED";

    private final ProcessDefinition definition = new ProcessDefinition();
    private final List<StepDefinition> steps = new ArrayList<>();
    private final List<MetricDefinition> metrics = new ArrayList<>();
    private final List<Definitions.Rule> rules;

    ProcessBuilder(String processCode, List<Definitions.Rule> rules) {
        definition.setProcessCode(processCode);
        definition.setStatus(CONFIRMED);
        this.rules = rules;
    }

    public ProcessBuilder name(String name) {
        definition.setProcessName(name);
        return this;
    }

    public ProcessBuilder description(String description) {
        definition.setProcessDescription(description);
        return this;
    }

    /**
     * The type of the entity followed, e.g. {@code "order"}.
     */
    public ProcessBuilder entityType(String entityType) {
        definition.setEntityType(entityType);
        return this;
    }

    /**
     * Selects the process handler and the pre- and post-processors; defaults to the process code.
     */
    public ProcessBuilder type(String processType) {
        definition.setProcessType(processType);
        return this;
    }

    public ProcessBuilder tags(String... tags) {
        definition.setTags(Arrays.asList(tags));
        return this;
    }

    public ProcessBuilder startsOn(String... eventCodes) {
        definition.setStartEventCodes(Arrays.asList(eventCodes));
        return this;
    }

    /**
     * Events that end the process explicitly, whatever its steps. Without them, it ends when its last mandatory
     * step completes.
     */
    public ProcessBuilder endsOn(String... eventCodes) {
        definition.setEndEventCodes(Arrays.asList(eventCodes));
        return this;
    }

    public ProcessBuilder cancelledOn(String... eventCodes) {
        definition.setCancelEventCodes(Arrays.asList(eventCodes));
        return this;
    }

    /**
     * The process's own deadline: this long after it starts, e.g. {@code "P1D"}.
     */
    public ProcessBuilder within(String isoDuration) {
        definition.setPlannedWithin(iso(isoDuration));
        return this;
    }

    public ProcessBuilder within(Duration duration) {
        definition.setPlannedWithin(duration.toString());
        return this;
    }

    public ProcessBuilder tolerance(String isoDuration) {
        definition.setTolerance(iso(isoDuration));
        return this;
    }

    /**
     * The next step: one of the tenant's shared steps, as it is.
     */
    public ProcessBuilder step(String stepCode) {
        final StepDefinition step = new StepDefinition();
        step.setStepCode(stepCode);
        steps.add(step);
        return this;
    }

    /**
     * The next step, defined here, or adapting the tenant's shared step of the same code.
     */
    public ProcessBuilder step(String stepCode, Consumer<StepBuilder> step) {
        final StepBuilder builder = new StepBuilder(stepCode, rules);
        step.accept(builder);
        steps.add(builder.build());
        return this;
    }

    /**
     * Plans the process's own time by a rule, e.g.
     * {@code planTime(order -> metadataTime(order, "createdAt").plusDays(1))}.
     */
    public ProcessBuilder planTime(Function<ProcessInstance, LocalDateTime> rule) {
        return plan("TIME", "TIMESTAMP", rule::apply);
    }

    /**
     * Plans a measurement of the process by a rule, computed when the process starts.
     *
     * @param rule returns the planned value; {@code String.valueOf} of it is kept
     */
    public ProcessBuilder plan(String measurementCode, String unit, Function<ProcessInstance, Object> rule) {
        planned(measurementCode);
        rules.add(new Definitions.Rule(measurementCode, unit, null, null, definition.getProcessCode(), rule));
        return this;
    }

    /**
     * A figure computed from the process's measurements when it completes, e.g.
     * {@code metric("FUEL_PER_KM", "FUEL / DISTANCE", m -> m.unit("L/KM").tolerance("10%").worseWhenHigher())}.
     */
    public ProcessBuilder metric(String code, String expression, Consumer<PlanBuilder> plan) {
        final PlanBuilder builder = new PlanBuilder();
        plan.accept(builder);
        final MetricDefinition metric = new MetricDefinition();
        metric.setCode(code);
        metric.setExpression(expression);
        final MeasurementDefinition shape = builder.definition();
        metric.setUnit(shape.getUnit());
        metric.setTolerance(shape.getTolerance());
        metric.setWorseWhen(shape.getWorseWhen());
        metrics.add(metric);
        return this;
    }

    ProcessDefinition build(String tenant) {
        definition.setTenant(tenant);
        definition.setSteps(steps);
        if (!measurements.isEmpty()) {
            definition.setMeasurements(measurements);
        }
        if (!metrics.isEmpty()) {
            definition.setMetrics(metrics);
        }
        return definition;
    }
}
