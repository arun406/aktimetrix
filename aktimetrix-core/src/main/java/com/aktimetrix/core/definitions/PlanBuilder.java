package com.aktimetrix.core.definitions;

import com.aktimetrix.core.referencedata.model.MeasurementDefinition;

/**
 * The plan of one measurement: its value, unit and tolerance.
 */
public final class PlanBuilder {
    private final MeasurementDefinition definition = new MeasurementDefinition();

    PlanBuilder() {
    }

    MeasurementDefinition definition() {
        return definition;
    }

    public PlanBuilder value(Object value) {
        definition.setValue(String.valueOf(value));
        return this;
    }

    public PlanBuilder unit(String unit) {
        definition.setUnit(unit);
        return this;
    }

    /**
     * How far the actual may deviate from the plan: an amount in the unit, e.g. {@code "5"}, or a percentage,
     * e.g. {@code "10%"}.
     */
    public PlanBuilder tolerance(String tolerance) {
        definition.setTolerance(tolerance);
        return this;
    }

    /**
     * Only an actual above the plan by more than the tolerance is out of tolerance, e.g. for a cost.
     */
    public PlanBuilder worseWhenHigher() {
        definition.setWorseWhen("HIGHER");
        return this;
    }

    /**
     * Only an actual below the plan by more than the tolerance is out of tolerance, e.g. for a rating.
     */
    public PlanBuilder worseWhenLower() {
        definition.setWorseWhen("LOWER");
        return this;
    }
}
