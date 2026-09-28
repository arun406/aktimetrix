package com.aktimetrix.core.referencedata.model;

import com.aktimetrix.core.api.MeasurementType;
import lombok.Data;

/**
 * A measurement declared on a step or process definition: its code and whether it is planned or actual.
 */
@Data
public class StepMeasurement {
    private String measurementCode;
    /**
     * {@code P}: planned, computed by a meter when the instance is created. {@code A}: actual, recorded when the step
     * or process completes.
     */
    private MeasurementType type;
    /**
     * For an actual measurement: where to read its value in the entity of the event that completes the step or
     * process, as a dot-separated path, e.g. {@code weightKg} or {@code delivery.distanceKm}. Without it, the meter
     * registered for the measurement computes the value.
     */
    private String valueFrom;
    /**
     * For an actual measurement read with {@link #valueFrom}: its unit, e.g. {@code KG}.
     */
    private String unit;
}
