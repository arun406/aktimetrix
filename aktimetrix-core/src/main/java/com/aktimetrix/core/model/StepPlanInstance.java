package com.aktimetrix.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent the step plan
 *
 * @author Arun.Kandakatla
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class StepPlanInstance {
    private String stepCode;
    private String stepInstanceId;
    private String processInstanceId;
    private List<MeasurementInstance> plannedMeasurements = new ArrayList<>();
    private String status;

    public StepPlanInstance(String stepCode, String stepInstanceId, String processInstanceId, List<MeasurementInstance> plannedMeasurements) {
        this.stepCode = stepCode;
        this.stepInstanceId = stepInstanceId;
        this.processInstanceId = processInstanceId;
        this.plannedMeasurements = plannedMeasurements;
    }
}
