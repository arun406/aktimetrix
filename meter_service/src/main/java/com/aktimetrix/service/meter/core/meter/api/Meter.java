package com.aktimetrix.service.meter.core.meter.api;

import com.aktimetrix.service.meter.core.model.MeasurementInstance;
import com.aktimetrix.service.meter.core.transferobjects.StepInstanceDTO;

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
    MeasurementInstance measure(String tenant, StepInstanceDTO step);
}
