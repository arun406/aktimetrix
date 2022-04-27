package com.aktimetrix.service.planner.service;

import com.aktimetrix.service.planner.model.StepInstance;
import com.aktimetrix.service.planner.repository.StepInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
    public void createStepInstances(List<StepInstance> stepInstances) {
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
    public StepInstance saveStepInstance(StepInstance stepInstance) {
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
    public List<StepInstance> getStepInstancesByProcessInstanceIdAndStepCode(String tenant, ObjectId processInstanceId, String stepCode) {
        return this.repository
                .findByTenantAndStepCodeAndProcessInstanceId(tenant, stepCode, processInstanceId);
    }


    /**
     * @param tenant
     * @param processInstanceId
     * @param stepCode
     * @param functionalCtx
     * @param groupCode
     * @param version
     * @param status
     * @return
     */
    public StepInstance prepareStepInstanceObject(String tenant, ObjectId processInstanceId, String stepCode,
                                                  String functionalCtx, String groupCode, String version, String status) {
        return new StepInstance(tenant, stepCode, processInstanceId, groupCode, functionalCtx, version, status, LocalDateTime.now());
    }

    /**
     * returns the step instance objects by process instance id
     *
     * @param tenant            tenant
     * @param processInstanceId process instance id
     * @return step instance collection
     */
    public List<StepInstance> getStepInstancesByProcessInstanceId(String tenant, ObjectId processInstanceId) {

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
        return this.repository.getStepInstancesWithMeasurements(tenant, new ObjectId(processInstanceId), new ObjectId(stepInstanceId));
    }
}
