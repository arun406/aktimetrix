package com.aktimetrix.core.definitions;

import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.util.Times;

import java.time.Instant;

/**
 * Helpers for planning rules written with the DSL.
 */
public final class Planning {

    private Planning() {
    }

    /**
     * Reads a date-time kept in the step's metadata, however the store returned it.
     *
     * @return the value, or {@code null} when the key is absent
     */
    public static Instant metadataTime(StepInstance step, String key) {
        return Times.toInstant(step.getMetadata() == null ? null : step.getMetadata().get(key));
    }

    /**
     * Reads a date-time kept in the process's metadata, however the store returned it.
     *
     * @return the value, or {@code null} when the key is absent
     */
    public static Instant metadataTime(ProcessInstance process, String key) {
        return Times.toInstant(process.getMetadata() == null ? null : process.getMetadata().get(key));
    }
}
