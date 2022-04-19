package com.aktimetrix.service.meter.core.meter.service;

import com.aktimetrix.service.meter.core.meter.repository.MeasurementInstanceRepository;
import com.aktimetrix.service.meter.core.model.MeasurementInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    /**
     * @param measurementInstances
     * @return
     */
    public List<MeasurementInstance> saveMeasurementInstances(List<MeasurementInstance> measurementInstances) {
        this.repository.saveAll(measurementInstances);
        return measurementInstances;
    }
}
