package com.aktimetrix.core.definitions;

import com.aktimetrix.core.meter.impl.AbstractMeter;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;

import java.time.LocalDateTime;

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
    public static LocalDateTime metadataTime(StepInstance step, String key) {
        return AbstractMeter.toLocalDateTime(step.getMetadata() == null ? null : step.getMetadata().get(key));
    }

    /**
     * Reads a date-time kept in the process's metadata, however the store returned it.
     *
     * @return the value, or {@code null} when the key is absent
     */
    public static LocalDateTime metadataTime(ProcessInstance process, String key) {
        return AbstractMeter.toLocalDateTime(process.getMetadata() == null ? null : process.getMetadata().get(key));
    }
}
