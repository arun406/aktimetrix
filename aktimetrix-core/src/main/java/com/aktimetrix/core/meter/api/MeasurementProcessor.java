package com.aktimetrix.core.meter.api;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.model.MeasurementInstance;

import java.util.List;

/**
 * Core API in the Meter Service.
 * All System should implement this service to capture the measurements for the step events.
 *
 * @author arun kumar kandakatla
 */
public interface MeasurementProcessor {

    /**
     * @param context meter context
     * @return list of measurement
     */
    List<MeasurementInstance> generateMeasurements(Context context);
}
