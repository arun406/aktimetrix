package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.repository.StepInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author arun kumar kandakatla
 */
@Service
@RequiredArgsConstructor
public class StepInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(StepInstanceService.class);
    final private StepInstanceRepository repository;

    /**
     * @param stepInstances step instance
     */
    public void save(List<StepInstance> stepInstances) {
        // Step Instance
        this.repository.saveAll(stepInstances);
        // log step instance ids;
        stepInstances.forEach(si -> logger.info(" Step Code: " + si.getStepCode() + ", Step instance id: " + si.getId()));
    }

    /**
     * persists the step instance
     *
     * @param stepInstance step instance
     * @return step instance
     */
    public StepInstance save(StepInstance stepInstance) {
        this.repository.save(stepInstance);
        return stepInstance;
    }


    /**
     * returns the step instance by process instance id and step code
     *
     * @param processInstanceId process instance id
     * @param stepCode          step code
     * @return step instance collection
     */
    public List<StepInstance> getStepInstancesByProcessInstanceIdAndStepCode(String tenant, String processInstanceId, String stepCode) {
        return this.repository
                .findByTenantAndStepCodeAndProcessInstanceId(tenant, stepCode, processInstanceId);
    }


    /**
     * Creates  step instances
     *
     * @param tenant          tenant
     * @param stepDefinitions step definitions
     * @param metadata        step metadata
     * @return step instance collection
     */
    public List<StepInstance> create(String tenant, List<StepDefinition> stepDefinitions,
                                     Map<String, Object> metadata) {

        List<StepInstance> steps = new ArrayList<>();
        for (StepDefinition stepDefinition : stepDefinitions) {
            final String stepCode = stepDefinition.getStepCode();
            final String groupCode = stepDefinition.getGroupCode();
            final String functionalCtx = stepDefinition.getFunctionalCtxCode();
            logger.info("step code: {}, step group code: {} ", stepCode, groupCode);
            StepInstance stepInstance = prepareStepInstanceObject(tenant, stepCode, functionalCtx,
                    groupCode, Constants.DEFAULT_VERSION, Constants.STEP_CREATED);
            stepInstance.setMetadata(metadata);
            steps.add(stepInstance);
        }
        return steps;
    }


    /**
     * @param itinerary
     * @param stepDefinition
     * @param stepInstance
     *//*
    private void setStepLocation(Itinerary itinerary, StepDefinition stepDefinition, StepInstance stepInstance) {
        if (StringUtils.equalsIgnoreCase("E", stepDefinition.getFunctionalCtxCode())) {
            stepInstance.setLocationCode(itinerary.getBoardPoint().getCode());
        } else if (StringUtils.equalsIgnoreCase("I", stepDefinition.getFunctionalCtxCode())) {
            stepInstance.setLocationCode(itinerary.getOffPoint().getCode());
        } else {
            // this need to corrected TODO
            if (StringUtils.equalsIgnoreCase(stepDefinition.getStepCode(), "DEP-T")) {
                stepInstance.setLocationCode(itinerary.getBoardPoint().getCode());
            } else {
                stepInstance.setLocationCode(itinerary.getOffPoint().getCode());
            }
        }
    }*/

    /**
     * @param tenant
     * @param stepCode
     * @param functionalCtx
     * @param groupCode
     * @param version
     * @param status
     * @return
     */
    public StepInstance prepareStepInstanceObject(String tenant, String stepCode,
                                                  String functionalCtx, String groupCode, String version, String status) {
        return new StepInstance(tenant, stepCode, groupCode, functionalCtx, version, status, LocalDateTime.now());
    }

    /**
     * returns the step instance objects by process instance id
     *
     * @param tenant            tenant
     * @param processInstanceId process instance id
     * @return step instance collection
     */
    public List<StepInstance> getStepInstancesByProcessInstanceId(String tenant, String processInstanceId) {

        Document params = new Document();
        params.put("tenant", tenant);
        params.put("processInstanceId", processInstanceId);

        return this.repository
                .findByTenantAndProcessInstanceId(tenant, processInstanceId);
    }

    /**
     * Returns step instance by process id and step id
     *
     * @param tenant            tenant
     * @param processInstanceId process instance id
     * @param stepInstanceId    step instance id
     * @return step instance
     */
    public StepInstance getStepInstancesByProcessInstanceIdAndId(String tenant, String processInstanceId, String stepInstanceId) {
        /*final StepInstance stepInstance = this.repository
                .getStepInstancesByProcessInstanceIdAndId(tenant, new ObjectId(processInstanceId), new ObjectId(stepInstanceId));
        final List<MeasurementInstance> stepMeasurements = this.measurementInstanceService
                .getStepMeasurements(tenant, new ObjectId(processInstanceId), new ObjectId(stepInstanceId));
        stepInstance.setMeasurements(stepMeasurements);
        return stepInstance;*/
        return this.repository.getStepInstancesWithMeasurements(tenant, processInstanceId, stepInstanceId);
    }
}
