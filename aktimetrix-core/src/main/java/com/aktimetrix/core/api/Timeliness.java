package com.aktimetrix.core.api;

/**
 * How a step's actual progress compares with its plan.
 */
public enum Timeliness {
    /**
     * The step has not completed yet and is forecast to miss its deadline, because an earlier step ran late.
     */
    AT_RISK,
    /**
     * The step completed by its deadline: its planned time plus any tolerance.
     */
    ON_TIME,
    /**
     * The step completed after its deadline.
     */
    LATE,
    /**
     * The deadline has passed and the step has not completed yet.
     */
    OVERDUE
}
