package com.aktimetrix.core.meter.api;

import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.transferobjects.Event;

/**
 * Meter records the quantity of something
 *
 * @author arun kumar kandakatla
 */
public interface Meter {

    /**
     * @param tenant tenant code
     * @param step   step instance
     * @return Measurement
     */
    MeasurementInstance measure(String tenant, StepInstance step);

    /**
     * The actual value of the measurement, when the step completes. Only called for actual ({@code A}) measurements
     * whose definition does not say where to read the value ({@code valueFrom}).
     *
     * @param event the event that completed the step
     * @return the measurement, or {@code null} for none
     */
    default MeasurementInstance measureActual(String tenant, StepInstance step, Event<?, ?> event) {
        return null;
    }
}
