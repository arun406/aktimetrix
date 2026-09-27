package com.aktimetrix.core.api;

/**
 * How a step's actual progress compares with its plan.
 */
public enum Timeliness {
    /**
     * The step completed at or before its planned time.
     */
    ON_TIME,
    /**
     * The step completed after its planned time.
     */
    LATE,
    /**
     * The planned time has passed and the step has not completed yet.
     */
    OVERDUE
}
