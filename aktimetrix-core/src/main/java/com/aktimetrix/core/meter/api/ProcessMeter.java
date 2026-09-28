package com.aktimetrix.core.meter.api;

import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;

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
}
