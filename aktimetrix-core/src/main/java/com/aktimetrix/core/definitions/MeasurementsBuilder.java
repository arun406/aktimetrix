package com.aktimetrix.core.definitions;

import com.aktimetrix.core.api.MeasurementType;
import com.aktimetrix.core.referencedata.model.MeasurementDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The measurements of a step or process, shared by {@link StepBuilder} and {@link ProcessBuilder}.
 */
public abstract class MeasurementsBuilder<B extends MeasurementsBuilder<B>> {

    MeasurementsBuilder() {
    }

    protected final List<MeasurementDefinition> measurements = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private B self() {
        return (B) this;
    }

    /**
     * A measurement with a fixed plan, e.g. {@code plan("DISTANCE", m -> m.value(5).unit("KM").tolerance("20%")
     * .worseWhenHigher())}.
     */
    public B plan(String measurementCode, Consumer<PlanBuilder> plan) {
        final PlanBuilder builder = new PlanBuilder();
        plan.accept(builder);
        final MeasurementDefinition definition = builder.definition();
        definition.setMeasurementCode(measurementCode);
        definition.setType(MeasurementType.P);
        measurements.add(definition);
        return self();
    }

    /**
     * An actual measurement read from the event that completes the step or process, e.g.
     * {@code actual("DISTANCE", "route.distanceKm", "KM")}.
     *
     * @param valueFrom where the value is in the event's entity, as a dot-separated path
     */
    public B actual(String measurementCode, String valueFrom, String unit) {
        final MeasurementDefinition definition = new MeasurementDefinition();
        definition.setMeasurementCode(measurementCode);
        definition.setType(MeasurementType.A);
        definition.setValueFrom(valueFrom);
        definition.setUnit(unit);
        measurements.add(definition);
        return self();
    }

    /**
     * A measurement with a fixed plan and its actual read from the completing event, in one go.
     */
    public B measure(String measurementCode, String valueFrom, Consumer<PlanBuilder> plan) {
        plan(measurementCode, plan);
        return actual(measurementCode, valueFrom, measurements.get(measurements.size() - 1).getUnit());
    }

    /**
     * Checks an ISO-8601 duration, e.g. {@code PT2H} or {@code P1D}, and keeps it as written.
     */
    static String iso(String duration) {
        java.time.Duration.parse(duration);
        return duration;
    }

    /**
     * A planned measurement whose value a planning rule computes, declared without a value.
     */
    protected void planned(String measurementCode) {
        final MeasurementDefinition definition = new MeasurementDefinition();
        definition.setMeasurementCode(measurementCode);
        definition.setType(MeasurementType.P);
        measurements.add(definition);
    }
}
