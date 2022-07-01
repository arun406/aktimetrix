package com.aktimetrix.core;

import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.StepPlanInstance;
import com.google.common.collect.ArrayListMultimap;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Tester {

    public static void main(String[] args) {
        ArrayListMultimap<String, StepPlanInstance> newStepsWithPlannedMeasurements = ArrayListMultimap.create();
        ArrayListMultimap<String, StepPlanInstance> oldStepsWithPlannedMeasurements = ArrayListMultimap.create();
        MeasurementInstance fwbPlanTime = new MeasurementInstance();
        fwbPlanTime.setId("2");
        fwbPlanTime.setTenant("XX");
        fwbPlanTime.setCode("TIME");
        fwbPlanTime.setStepCode("FWB");
        fwbPlanTime.setMeasuredAt("DXB");
        fwbPlanTime.setUnit("TIMESTAMP");
        fwbPlanTime.setValue("2021-06-25T12:40:00");
        fwbPlanTime.setType("P");
        fwbPlanTime.setStepCode("1");
        fwbPlanTime.setProcessInstanceId("1");

        MeasurementInstance depTPlanTime = new MeasurementInstance();
        depTPlanTime.setId("3");
        depTPlanTime.setTenant("XX");
        depTPlanTime.setCode("TIME");
        depTPlanTime.setStepCode("DEP");
        depTPlanTime.setMeasuredAt("DXB");
        depTPlanTime.setUnit("TIMESTAMP");
        depTPlanTime.setValue("2021-06-25T12:40:00");
        depTPlanTime.setType("P");
        depTPlanTime.setStepCode("1");
        depTPlanTime.setProcessInstanceId("1");

        MeasurementInstance oldfwbPlanTime = new MeasurementInstance();
        oldfwbPlanTime.setId("1");
        oldfwbPlanTime.setTenant("XX");
        oldfwbPlanTime.setCode("TIME");
        oldfwbPlanTime.setStepCode("FWB");
        oldfwbPlanTime.setMeasuredAt("SHJ");
        oldfwbPlanTime.setUnit("TIMESTAMP");
        oldfwbPlanTime.setValue("2021-06-25T12:40:00");
        oldfwbPlanTime.setType("P");
        oldfwbPlanTime.setStepCode("1");
        oldfwbPlanTime.setProcessInstanceId("1");

        MeasurementInstance fwbPlanPcs = new MeasurementInstance();
        fwbPlanPcs.setId("4");
        fwbPlanPcs.setTenant("XX");
        fwbPlanPcs.setCode("PCS");
        fwbPlanPcs.setStepCode("FWB");
        fwbPlanPcs.setMeasuredAt("DXB");
        fwbPlanPcs.setUnit("NUMBER");
        fwbPlanPcs.setValue("10");
        fwbPlanPcs.setType("P");
        fwbPlanPcs.setStepCode("1");
        fwbPlanPcs.setProcessInstanceId("1");

        List<MeasurementInstance> measurementInstanceList = new ArrayList<>();
        List<MeasurementInstance> depTmeasurementInstanceList = new ArrayList<>();
        measurementInstanceList.add(fwbPlanTime);
        measurementInstanceList.add(fwbPlanPcs);

        depTmeasurementInstanceList.add(depTPlanTime);

        List<MeasurementInstance> oldMeasurementInstanceList = new ArrayList<>();
        oldMeasurementInstanceList.add(oldfwbPlanTime);

        StepPlanInstance fwbStepPlanInstance = new StepPlanInstance("FWB", "1", "1", measurementInstanceList);
        newStepsWithPlannedMeasurements.put("FWB", fwbStepPlanInstance);

        StepPlanInstance depTStepPlanInstance = new StepPlanInstance("FWB", "2", "1", depTmeasurementInstanceList);
        newStepsWithPlannedMeasurements.put("DEP", depTStepPlanInstance);

        StepPlanInstance oldfwbStepPlanInstance = new StepPlanInstance("FWB", "2", "2", oldMeasurementInstanceList);
        newStepsWithPlannedMeasurements.put("FWB", fwbStepPlanInstance);

        oldStepsWithPlannedMeasurements.put("FWB", oldfwbStepPlanInstance);

        Tester tester = new Tester();
        tester.print(newStepsWithPlannedMeasurements);
        newStepsWithPlannedMeasurements.forEach((stepCode, stepPlanInstance) -> {
            LocalDateTime newPlanTime = stepPlanInstance.getPlannedMeasurements().stream()
                    .filter(mi -> mi.getCode().equals("TIME"))
                    .map(mi -> LocalDateTime.parse(mi.getValue(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))
                    .findFirst().get();
            String newMeasuredAt = stepPlanInstance.getPlannedMeasurements().stream()
                    .filter(mi -> mi.getCode().equals("TIME"))
                    .map(MeasurementInstance::getMeasuredAt)
                    .findFirst().get();

            if (!oldStepsWithPlannedMeasurements.containsKey(stepCode)) {
                stepPlanInstance.setStatus("N");
            } else {
                // check the plantime measurement.
                List<StepPlanInstance> stepPlanInstances = oldStepsWithPlannedMeasurements.get(stepCode);
                for (StepPlanInstance oldPlanInstance : stepPlanInstances) {
                    log.debug("iterating for step :{}", stepCode);
                    // check actual received for the step code
                    String oldStepId = oldPlanInstance.getStepInstanceId();
                    String oldProcessId = oldPlanInstance.getProcessInstanceId();
                    log.debug("new process instance id: {}, new step instance id: {}", oldProcessId, oldStepId);
                    boolean completed = false;
//                            this.planner.isStepCompleted(tenant, oldProcessId, oldStepId);
                    log.debug("is step completed :{}", completed);
                    if (completed) {
                        stepPlanInstance.setStatus("P");
                        // preceding steps status should be set to "P".
                    }


                    // status "N"
                    List<MeasurementInstance> plannedMeasurements = oldPlanInstance.getPlannedMeasurements();
                    for (MeasurementInstance plannedMeasurement : plannedMeasurements) {
                        if (plannedMeasurement.getCode().equals("TIME")) {
                            String measuredAt = plannedMeasurement.getMeasuredAt();
                            String value = plannedMeasurement.getValue();
                            LocalDateTime planTime = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                            if (!planTime.isEqual(newPlanTime) || !measuredAt.equals(newMeasuredAt)) {
                                stepPlanInstance.setStatus("N");
                            }
                        }
                    }
                }
            }
            LocalDateTime eventTimestamp = LocalDateTime.now();
            if (newPlanTime.isBefore(eventTimestamp)) {
                stepPlanInstance.setStatus("P");
            }

        });
        tester.print(newStepsWithPlannedMeasurements);
    }

    private void print(ArrayListMultimap<String, StepPlanInstance> newStepsWithPlannedMeasurements) {
        if (newStepsWithPlannedMeasurements != null) {
            log.debug("steps with planned measurements size : {}", newStepsWithPlannedMeasurements.size());
            newStepsWithPlannedMeasurements.entries().stream().forEach(entry -> {
                log.debug("entry: {}, and the value : {} ", entry.getKey(), entry.getValue());
            });
        }
    }
}
