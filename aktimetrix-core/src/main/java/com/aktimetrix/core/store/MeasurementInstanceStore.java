package com.aktimetrix.core.store;

import com.aktimetrix.core.model.MeasurementInstance;

import java.util.List;

/**
 * Stores measurement instances. They are written once and never updated.
 */
public interface MeasurementInstanceStore {

    /**
     * Inserts the measurement, assigning it an id.
     */
    MeasurementInstance save(MeasurementInstance measurement);

    default List<MeasurementInstance> saveAll(List<MeasurementInstance> measurements) {
        measurements.forEach(this::save);
        return measurements;
    }

    /**
     * Every measurement of the process instance, of the process itself and of its steps.
     */
    List<MeasurementInstance> findByProcessInstance(String tenant, String processInstanceId);

    /**
     * The measurements with the code and type ({@code P} or {@code A}) of one step, or, with a {@code null}
     * {@code stepInstanceId}, of the process itself.
     */
    List<MeasurementInstance> find(String tenant, String processInstanceId, String stepInstanceId, String code,
                                   String type);
}
