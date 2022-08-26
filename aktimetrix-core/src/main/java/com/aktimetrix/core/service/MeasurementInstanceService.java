package com.aktimetrix.core.service;

import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.StepMeasurement;
import com.aktimetrix.core.referencedata.service.StepDefinitionService;
import com.aktimetrix.core.repository.MeasurementInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
    private final StepDefinitionService stepDefinitionService;

    /**
     * save single measurement instance
     *
     * @param measurementInstance
     * @return
     */

    public MeasurementInstance saveMeasurementInstance(MeasurementInstance measurementInstance) {
        this.repository.save(measurementInstance);
        return measurementInstance;
    }

    /**
     * Save the measurement instances
     *
     * @param measurementInstances
     * @return
     */
    public List<MeasurementInstance> saveMeasurementInstances(List<MeasurementInstance> measurementInstances) {
        this.repository.saveAll(measurementInstances);
        return measurementInstances;
    }

    /**
     * return all process measurements
     *
     * @param tenant
     * @param processInstanceId
     * @return
     */
    public List<MeasurementInstance> getProcessMeasurements(String tenant, String processInstanceId) {
        return this.repository.findByProcessInstanceId(tenant, processInstanceId);
    }

    /**
     * return all step measurements
     *
     * @param tenant
     * @param processInstanceId
     * @param stepInstanceId
     * @return
     */
    public List<MeasurementInstance> getStepMeasurements(String tenant, String processInstanceId, String stepInstanceId) {
        return this.repository.findByProcessInstanceIdAndStepInstanceId(tenant, processInstanceId, stepInstanceId);
    }

    /**
     * return planned measurements
     *
     * @param tenant
     * @param processInstanceId
     * @return
     */
    public List<MeasurementInstance> getPlannedMeasurements(String tenant, String processInstanceId) {
        return this.repository.findByProcessInstanceIdAndType(tenant, processInstanceId, "P");
    }

    /**
     * return actual measurements
     *
     * @param tenant
     * @param processInstanceId
     * @return
     */
    public List<MeasurementInstance> getActualMeasurements(String tenant, String processInstanceId) {
        return this.repository.findByProcessInstanceIdAndType(tenant, processInstanceId, "A");
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
        Map<String, List<String>> expected = processInstance.getSteps().stream()
                .collect(Collectors.toMap(StepInstance::getStepCode,
                        stepInstance -> this.stepDefinitionService.get(tenant, stepInstance.getStepCode())
                                .getMeasurements()
                                .stream()
                                .filter(sm -> measurementType.equals(sm.getType().name()))
                                .map(StepMeasurement::getMeasurementCode)
                                .collect(toList())));
        log.info("expected: {}", expected);

        Map<String, List<String>> actual = measurementInstances.stream()
                .filter(m -> measurementType.equals(m.getType()))
                .collect(Collectors.groupingBy(MeasurementInstance::getStepCode,
                        mapping(MeasurementInstance::getCode, toList())));

        log.info("actual: {}", actual);
        if (expected.size() != actual.size()) {
            return false;
        }

        return expected.entrySet().stream()
                .allMatch(e -> CollectionUtils.isEqualCollection(e.getValue(), actual.get(e.getKey())));
    }

    /**
     * check all step measurements are received or not
     *
     * @param tenant
     * @param processInstance
     * @param stepInstanceId
     * @param measurementType
     * @return
     */
    public boolean isAllMeasurementsCaptured(String tenant, ProcessInstance processInstance, String stepInstanceId, String measurementType) {
        log.debug("steps: {}", processInstance.getSteps());
        if (processInstance.getSteps().isEmpty()) {
            return false;
        }
        List<MeasurementInstance> measurementInstances =
                this.getStepMeasurements(tenant, processInstance.getId(), stepInstanceId);
        if (measurementInstances.isEmpty()) {
            return false;
        }
        Map<String, List<String>> expected = processInstance.getSteps().stream()
                .filter(si -> si.getId().equals(stepInstanceId))
                .collect(Collectors.toMap(StepInstance::getStepCode,
                        si -> this.stepDefinitionService.get(tenant, si.getStepCode())
                                .getMeasurements()
                                .stream()
                                .filter(sm -> measurementType.equals(sm.getType().name()))
                                .map(StepMeasurement::getMeasurementCode)
                                .collect(toList())));
        log.info("expected: {}", expected);

        Map<String, List<String>> actual = measurementInstances.stream()
                .filter(m -> measurementType.equals(m.getType()))
                .collect(Collectors.groupingBy(MeasurementInstance::getStepCode,
                        mapping(MeasurementInstance::getCode, toList())));

        log.info("actual: {}", actual);
        if (expected.size() != actual.size()) {
            return false;
        }

        return expected.entrySet().stream()
                .allMatch(e -> CollectionUtils.isEqualCollection(e.getValue(), actual.get(e.getKey())));
    }

    /**
     * returns step measurements
     *
     * @param processInstanceId
     * @param stepInstanceId
     * @param measurementType
     * @return
     */
    public List<MeasurementInstance> getStepMeasurements(String tenant, String processInstanceId, String stepInstanceId,
                                                         String measurementType) {
        return this.repository
                .findByProcessInstanceIdAndStepInstanceIdAndType(tenant, processInstanceId, stepInstanceId, measurementType);
    }

    /**
     * @param tenant
     * @param entityId
     * @param entityType
     * @param stepCode
     * @return
     */
    public boolean isActualMeasurementsAvailableForStep(String tenant, String entityId, String entityType, String stepCode) {

        List<MeasurementInstance> actual = this.repository
                .findActualByEntityIdAndEntityTypeAndStepCode(tenant, entityId, entityType, stepCode);
        return actual != null && !actual.isEmpty();
    }
}
