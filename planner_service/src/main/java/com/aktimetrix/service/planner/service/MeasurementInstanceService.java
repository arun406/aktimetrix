package com.aktimetrix.service.planner.service;

import com.aktimetrix.service.planner.model.MeasurementInstance;
import com.aktimetrix.service.planner.repository.MeasurementInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeasurementInstanceService {
    private final MeasurementInstanceRepository repository;

    /**
     * @param measurementInstance entity to save
     * @return saved entity
     */

    public MeasurementInstance saveMeasurementInstance(MeasurementInstance measurementInstance) {
        this.repository.save(measurementInstance);
        return measurementInstance;
    }

    public List<MeasurementInstance> getProcessMeasurements(String tenant, ObjectId processInstanceId) {
        return this.repository.findByProcessInstanceId(tenant, processInstanceId);
    }
}
