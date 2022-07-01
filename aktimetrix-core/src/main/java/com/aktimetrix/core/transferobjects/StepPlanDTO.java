package com.aktimetrix.core.transferobjects;

import lombok.*;

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
@Builder
public class StepPlanDTO {
    private String stepCode;
    private String stepInstanceId;
    private String processInstanceId;
    private List<Measurement> plannedMeasurements = new ArrayList<>();
    private String status;

    public StepPlanDTO(String stepCode, String stepInstanceId, String processInstanceId, List<Measurement> plannedMeasurements) {
        this.stepCode = stepCode;
        this.stepInstanceId = stepInstanceId;
        this.processInstanceId = processInstanceId;
        this.plannedMeasurements = plannedMeasurements;
    }
}
