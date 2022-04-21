package com.aktimetrix.service.meter.core.meter.api;

import com.aktimetrix.service.meter.core.transferobjects.Measurement;
import com.aktimetrix.service.meter.core.transferobjects.StepInstanceDTO;

import java.util.List;

/**
 * Core API in the Meter Service.
 * All System should implement this service to capture the measurements for the step events.
 *
 * @author arun kumar kandakatla
 */
public interface MeasurementService {

    /**
     * @param tenantKey tenant
     * @param step      step
     * @return list of measurement
     */
    List<Measurement> generateMeasurements(String tenantKey, StepInstanceDTO step);
}
