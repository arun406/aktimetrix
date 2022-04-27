package com.aktimetrix.service.processor.core.resource;

import com.aktimetrix.service.processor.core.model.ProcessInstance;
import com.aktimetrix.service.processor.core.model.StepInstance;
import com.aktimetrix.service.processor.core.referencedata.model.StepDefinition;
import com.aktimetrix.service.processor.core.referencedata.service.StepDefinitionService;
import com.aktimetrix.service.processor.core.service.ProcessInstanceService;
import com.aktimetrix.service.processor.core.service.StepInstanceService;
import com.aktimetrix.service.processor.core.transferobjects.Process;
import com.aktimetrix.service.processor.core.transferobjects.Step;
import com.aktimetrix.service.processor.core.util.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequestMapping("/svm/process-instances/")
@RestController
@Slf4j
public class ProcessInstancesResource {

    private Map<String, StepDefinition> stepDefinitionMap = new HashMap<>();

    final private StepInstanceService stepInstanceService;
    final private ProcessInstanceService processInstanceService;
    final private StepDefinitionService stepDefinitionService;

    /**
     * constructor
     *
     * @param stepDefinitionService
     */
    @Autowired
    public ProcessInstancesResource(StepDefinitionService stepDefinitionService, StepInstanceService stepInstanceService,
                                    ProcessInstanceService processInstanceService) {
        this.stepInstanceService = stepInstanceService;
        this.processInstanceService = processInstanceService;
        this.stepDefinitionService = stepDefinitionService;
        this.stepDefinitionMap = getStepDefinitionMap();
    }

    @GetMapping("/{process-instance-id}/step-instances")
    public ResponseEntity getStepInstances(@PathVariable("process-instance-id") String processInstanceId) {
        List<Step> steps = null;
        log.debug(" Process Instance Id: {}", processInstanceId);
        steps = getSteps(processInstanceId);
        return ResponseEntity.ok(steps);
    }

    @GetMapping("/{process-instance-id}/step-instances/{step-instance-id}")
    public ResponseEntity getStepInstance(@PathVariable("process-instance-id") String processInstanceId,
                                          @PathVariable("step-instance-id") String stepInstanceId) {

        log.debug(" Process Instance Id: {}, Step Instance Id: {}", processInstanceId, stepInstanceId);
        Step step = getStep("XX", processInstanceId, stepInstanceId);
        return ResponseEntity.ok(step);
    }

    @GetMapping("/{process-instance-id}")
    public ResponseEntity getProcessInstance(@PathVariable("process-instance-id") String processInstanceId) {

        final ProcessInstance processInstance = this.processInstanceService
                .getProcessInstance("XX", new ObjectId(processInstanceId));
        Process process = convertToProcess(processInstanceId, processInstance);
        return ResponseEntity.ok(process);
    }


    /**
     * @param processInstanceId
     * @return
     */
    private List<Step> getSteps(String processInstanceId) {

        List<Step> steps = new ArrayList<>();

        final List<StepInstance> stepInstances = this.stepInstanceService
                .getStepInstancesByProcessInstanceId("XX", new ObjectId(processInstanceId));
        if (CollectionUtil.isEmptyOrNull(stepInstances)) {
            return steps;
        }
        steps = stepInstances.stream()
//                .filter(si -> !"BKD".equalsIgnoreCase(si.getStepCode()))
                .map(si -> this.convertToStep(si))
                .collect(Collectors.toList());
        return steps;
    }

    /**
     * @param tenant
     * @param processInstanceId
     * @param stepInstanceId
     * @return
     */
    private Step getStep(String tenant, String processInstanceId, String stepInstanceId) {
        StepInstance si = this.stepInstanceService.getStepInstancesByProcessInstanceIdAndId(tenant, processInstanceId, stepInstanceId);
        final StepDefinition definition = getStepDefinitionMap().get(si.getStepCode());
        Step step = this.convertToStep(si);
        return step;
    }

    /**
     * Convert the StepInstance To Step
     *
     * @param si
     * @return
     */
    private Step convertToStep(StepInstance si) {

        StepDefinition definition = stepDefinitionMap.get(si.getStepCode());
        Step step = new Step();
        step.setTenant(si.getTenant());
        step.setId(si.getId().toString());
        step.setCode(si.getStepCode());
        step.setGroupCode(si.getGroupCode());
        step.setFunctionalCtxCode(si.getFunctionalCtx());
        step.setLocationCode(si.getLocationCode());
        step.setVersion(Integer.parseInt(si.getVersion()));
        step.setStatus(si.getStatus());
        step.setCreatedOn(si.getCreatedOn());
        step.setMetadata(si.getMetadata());
        if (definition != null) {
            step.setLocationalCtxCode(definition.getLocationCtxCode());
            step.setName(definition.getStepName());
            step.setResponsiblePartyCode(definition.getResponsiblePartyCode());
            step.setCategoryCode(definition.getCategoryCode());
            step.setSubCategoryCode(definition.getSubCategoryCode());
        }
        /*if (si.getMeasurements() != null && !si.getMeasurements().isEmpty()) {
            final List<Measurement> measurements = si.getMeasurements().stream()
                    .map(this::convertToMeasurement)
                    .collect(Collectors.toList());
            step.setMeasurements(measurements);
        }*/
        return step;
    }

    /*  *//**
     * Convert to Measurement Object
     *
     * @param mi
     * @return
     *//*
    private Measurement convertToMeasurement(MeasurementInstance mi) {
        Measurement measurement = new Measurement();
        measurement.setCode(mi.getCode());
        measurement.setTenant(mi.getTenant());
        measurement.setUnit(mi.getUnit());
        measurement.setType(mi.getType());
        measurement.setCreatedOn(mi.getCreatedOn());
        measurement.setMeasuredAt(mi.getMeasuredAt());
        measurement.setValue(mi.getValue());
        return measurement;
    }*/

    /**
     * @return
     */
    private Map<String, StepDefinition> getStepDefinitionMap() {

        final Map<String, StepDefinition> stepDefinitionMap = this.stepDefinitionService.list()
                .stream()
                .collect(Collectors.toMap(StepDefinition::getStepCode, Function.identity()));
        log.debug(" Step Definition Map:  {} ", stepDefinitionMap);
        return stepDefinitionMap;
    }

    /**
     * Convert To Process Object
     *
     * @param processInstanceId
     * @param processInstance
     */
    private Process convertToProcess(String processInstanceId, ProcessInstance processInstance) {
        Process process = new Process();
        process.setId(processInstance.getId().toString());
        process.setProcessCode(processInstance.getProcessCode());
        process.setCategoryCode(processInstance.getCategoryCode());
        process.setSubCategoryCode(processInstance.getSubCategoryCode());
        process.setActive(false);
        process.setMetadata(processInstance.getMetadata());
        process.setEntityId(processInstance.getEntityId());
        process.setEntityType(processInstance.getEntityType());
        process.setStatus(processInstance.getStatus());
        process.setCreatedOn(processInstance.getCreatedOn());
        process.setVersion(processInstance.getVersion());
        process.setTenant(processInstance.getTenant());
//        process.setQualified(false);
//        process.setPartner(false);
        process.setValid(false);
        process.setSteps(getSteps(processInstanceId));
        return process;
    }

}
