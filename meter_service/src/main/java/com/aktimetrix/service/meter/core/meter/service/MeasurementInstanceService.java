package com.aktimetrix.service.meter.core.meter.service;

import com.aktimetrix.service.meter.core.model.MeasurementInstance;
import com.aktimetrix.service.meter.core.meter.repository.MeasurementInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeasurementInstanceService {
    private final MeasurementInstanceRepository repository;

    /**
     * @param measurementInstance
     * @return
     */

    public MeasurementInstance saveMeasurementInstance(MeasurementInstance measurementInstance) {
        this.repository.save(measurementInstance);
        return measurementInstance;
    }
}
