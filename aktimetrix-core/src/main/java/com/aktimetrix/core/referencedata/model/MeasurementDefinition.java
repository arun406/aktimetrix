package com.aktimetrix.core.referencedata.model;

import com.aktimetrix.core.api.MeasurementType;
import lombok.Data;

/**
 * A measurement declared on a step or process definition: what to measure ({@code measurementCode}, a
 * user-defined dimension such as {@code TIME}, {@code DISTANCE} or {@code RATING}), and whether the value is planned
 * or actual.
 */
@Data
public class MeasurementDefinition {
    private String measurementCode;
    /**
     * {@code P}: planned, computed by a meter when the instance is created. {@code A}: actual, recorded when the step
     * or process completes.
     */
    private MeasurementType type;
    /**
     * For a planned measurement: a fixed planned value, e.g. {@code 5} for a customer rating, used when no meter is
     * registered for the measurement. Plans that depend on the entity, such as a shorter delivery for priority
     * customers, are computed by a meter instead.
     */
    private String value;
    /**
     * How far the actual value may differ from the planned value, either way: an absolute amount in the measurement's
     * unit, e.g. {@code 2}, or a percentage of the planned value, e.g. {@code 10%}. Declared on the planned or the
     * actual measurement; without it, the difference is recorded but not judged.
     */
    private String tolerance;
    /**
     * For an actual measurement: where to read its value in the entity of the event that completes the step or
     * process, as a dot-separated path, e.g. {@code weightKg} or {@code delivery.distanceKm}. Without it, the meter
     * registered for the measurement computes the value.
     */
    private String valueFrom;
    /**
     * The unit of a value given here or read with {@link #valueFrom}, e.g. {@code KG}.
     */
    private String unit;
}
