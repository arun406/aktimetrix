package com.aktimetrix.core.api;

/**
 * How an actual measurement compares with its plan, given the tolerance declared on the measurement. For {@code TIME},
 * a step's {@link Timeliness} says more: it also covers steps that have not happened yet.
 */
public enum Conformance {
    /**
     * The actual value differs from the planned value by no more than the tolerance.
     */
    WITHIN_TOLERANCE,
    /**
     * The actual value differs from the planned value by more than the tolerance.
     */
    OUT_OF_TOLERANCE
}
