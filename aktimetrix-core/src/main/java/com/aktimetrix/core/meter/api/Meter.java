package com.aktimetrix.core.meter.api;

import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.StepInstance;

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
}
