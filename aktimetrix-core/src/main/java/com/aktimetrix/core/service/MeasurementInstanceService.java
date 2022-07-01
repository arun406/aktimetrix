package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.StepMeasurement;
import com.aktimetrix.core.referencedata.service.StepDefinitionService;
import com.aktimetrix.core.repository.MeasurementInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeasurementInstanceService {
    private final MeasurementInstanceRepository repository;
    @Autowired
    private StepDefinitionService stepDefinitionService;

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

    public List<MeasurementInstance> getProcessMeasurements(String tenant, String processInstanceId) {
        return this.repository.findByProcessInstanceId(tenant, processInstanceId);
    }

    public List<MeasurementInstance> getPlannedMeasurements(String tenant, String processInstanceId) {
        return this.repository.findByProcessInstanceIdAndType(tenant, processInstanceId, "P");
    }

    /**
     * When event process events are received, this method will be called to check all planned measurements for all
     * steps of the given process instances are available in the system
     * <br>
     *
     * @return
     */
    public boolean isAllMeasurementsCaptured(String tenant, ProcessInstance processInstance, String measurementType) {
        log.debug("steps: {}", processInstance.getSteps());
        if (processInstance.getSteps().isEmpty()) {
            return false;
        }
        List<MeasurementInstance> measurementInstances = this.getProcessMeasurements(tenant, processInstance.getId());
        if (measurementInstances.isEmpty()) {
            return false;
        }
        Map<String, List<String>> expected = processInstance.getSteps()
                .stream().collect(Collectors.toMap(StepInstance::getStepCode,
                        stepInstance -> stepDefinitionService.get(tenant, stepInstance.getStepCode())
                                .getMeasurements()
                                .stream()
                                .filter(sm -> Constants.PLAN_MEASUREMENT_TYPE.equals(sm.getType().name()))
                                .map(StepMeasurement::getMeasurementCode)
                                .collect(toList())));
        log.debug("expected: {}", expected);
        Map<String, List<String>> actual = measurementInstances.stream()
                .collect(Collectors.groupingBy(MeasurementInstance::getStepCode,
                        mapping(MeasurementInstance::getCode, toList())));

        log.debug("actual: {}", actual);
        if (expected.size() != actual.size()) {
            return false;
        }

        return expected.entrySet().stream()
                .allMatch(e -> CollectionUtils.isEqualCollection(e.getValue(), actual.get(e.getKey())));

       /* final List<String> expectedSteps = processInstance.getSteps()
                .stream()
                .filter(s -> {
                    List<StepMeasurement> measurements = stepDefinitionService.get(tenant, s.getStepCode()).getMeasurements();
                    log.debug("measurement definition of step: {}  is {}", s.getStepCode(), measurements);
                    if (!measurements.isEmpty()) {
                        List<StepMeasurement> planMeasurements = measurements.stream()
                                .filter(sm -> {
                                    log.debug("measurement code:{}, type :{}", sm.getMeasurementCode(), sm.getType());
                                    return Constants.PLAN_MEASUREMENT_TYPE.equals(sm.getType().name());
                                })
                                .collect(toList());
                        log.debug("plan measurement :{}", planMeasurements);
                        return !planMeasurements.isEmpty();
                    }
                    return false;
                })
                .map(StepInstance::getId)
                .collect(toList());

        final boolean allMatch = measurementInstances.stream()
                .filter(mi -> mi.getType().equalsIgnoreCase(measurementType))
                .map(MeasurementInstance::getStepInstanceId)
                .collect(Collectors.toSet()).stream()
                .allMatch(expectedSteps::remove);

        //log.debug("all step measurements are available.");
        return allMatch && expectedSteps.isEmpty();*/
    }


    /**
     * @param processInstanceId
     * @param stepInstanceId
     * @param measurementType
     * @return
     */
    public List<MeasurementInstance> getStepMeasurements(String tenant, String processInstanceId, String stepInstanceId, String measurementType) {
        return this.repository.findByProcessInstanceIdAndStepInstanceIdAndType(tenant, processInstanceId, stepInstanceId, measurementType);
    }

    /**
     * @param tenant
     * @param entityId
     * @param entityType
     * @param stepCode
     * @return
     */
    public boolean isActualMeasurementsAvailableForStep(String tenant, String entityId, String entityType, String stepCode) {

        List<MeasurementInstance> actual = this.repository.findActualByEntityIdAndEntityTypeAndStepCode(tenant, entityId, entityType, stepCode);
        if (actual != null && !actual.isEmpty()) {
            return true;
        }
        return false;
    }
}
