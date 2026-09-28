package com.aktimetrix.core.referencedata.model;

import lombok.Data;

/**
 * A metric of a process computed from its measurements when the process completes, e.g. fuel per kilometre:
 * {@code { "code": "FUEL_PER_KM", "expression": "FUEL / DISTANCE", "unit": "L/KM" }}.
 * <p>
 * In the expression, a measurement code stands for the sum of that measurement's values across the process instance
 * and its steps. The metric is computed from the actual values and from the planned values, and the two are compared
 * like any measurement.
 */
@Data
public class MetricDefinition {
    /**
     * The metric's code, e.g. {@code FUEL_PER_KM}.
     */
    private String code;
    /**
     * Arithmetic over measurement codes and numbers: {@code + - * /} and parentheses, e.g. {@code FUEL / DISTANCE}.
     */
    private String expression;
    private String unit;
    /**
     * As for a measurement: {@code 2} or {@code 10%}.
     */
    private String tolerance;
    /**
     * As for a measurement: {@code HIGHER} or {@code LOWER}.
     */
    private String worseWhen;
}
