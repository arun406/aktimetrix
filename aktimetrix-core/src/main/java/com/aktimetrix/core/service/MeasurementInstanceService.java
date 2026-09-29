package com.aktimetrix.core.service;

import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.store.MeasurementInstanceStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeasurementInstanceService {
    private final MeasurementInstanceStore store;

    public MeasurementInstance saveMeasurementInstance(MeasurementInstance measurementInstance) {
        return store.save(measurementInstance);
    }

    public List<MeasurementInstance> saveMeasurementInstances(List<MeasurementInstance> measurementInstances) {
        return store.saveAll(measurementInstances);
    }
}
