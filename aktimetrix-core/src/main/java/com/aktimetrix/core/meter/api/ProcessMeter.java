package com.aktimetrix.core.meter.api;

import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.transferobjects.Event;

/**
 * Records a quantity of a process as a whole, such as its total distance, rather than of one of its steps.
 *
 * @see Meter
 */
public interface ProcessMeter {

    /**
     * @param tenant  tenant code
     * @param process process instance
     * @return Measurement
     */
    MeasurementInstance measure(String tenant, ProcessInstance process);

    /**
     * The actual value of the measurement, when the process completes. Only called for actual ({@code A})
     * measurements whose definition does not say where to read the value ({@code valueFrom}).
     *
     * @param event the event that completed the process
     * @return the measurement, or {@code null} for none
     */
    default MeasurementInstance measureActual(String tenant, ProcessInstance process, Event<?, ?> event) {
        return null;
    }
}
