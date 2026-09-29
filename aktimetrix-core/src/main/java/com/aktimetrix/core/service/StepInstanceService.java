package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.store.StepInstanceStore;
import lombok.RequiredArgsConstructor;
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
    private final StepInstanceStore store;

    /**
     * @param stepInstances step instances to save
     */
    public void save(List<StepInstance> stepInstances) {
        store.saveAll(stepInstances);
        stepInstances.forEach(si -> logger.info(" Step Code: " + si.getStepCode() + ", Step instance id: " + si.getId()));
    }

    /**
     * Saves the step instance: inserts it, assigning its id, or updates it with a version check.
     */
    public StepInstance save(StepInstance stepInstance) {
        store.save(stepInstance);
        return stepInstance;
    }

    /**
     * Creates the step instances of a process instance, one per step definition, in order.
     *
     * @param tenant            tenant
     * @param stepDefinitions   step definitions
     * @param metadata          step metadata
     * @param processInstanceId process instance id
     * @return step instance collection
     */
    public List<StepInstance> save(String tenant, List<StepDefinition> stepDefinitions,
                                   Map<String, Object> metadata, String processInstanceId) {
        List<StepInstance> steps = new ArrayList<>();
        int sequence = 0;
        for (StepDefinition stepDefinition : stepDefinitions) {
            final String stepCode = stepDefinition.getStepCode();
            final String groupCode = stepDefinition.getGroupCode();
            final String functionalCtx = stepDefinition.getFunctionalCtxCode();
            logger.info("step code: {}, step group code: {} ", stepCode, groupCode);
            StepInstance stepInstance = prepareStepInstanceObject(tenant, processInstanceId, stepCode, functionalCtx,
                    groupCode, Constants.DEFAULT_VERSION, Constants.STATUS_CREATED);
            stepInstance.setMetadata(metadata);
            stepInstance.setSequence(sequence++);
            steps.add(stepInstance);
        }
        this.save(steps);
        return steps;
    }

    public StepInstance prepareStepInstanceObject(String tenant, String processInstanceId, String stepCode,
                                                  String functionalCtx, String groupCode, String version, String status) {
        return new StepInstance(tenant, stepCode, processInstanceId, groupCode, functionalCtx, version, status, LocalDateTime.now());
    }

    /**
     * Returns the step instances of the process instance, in order.
     */
    public List<StepInstance> getStepInstancesByProcessInstanceId(String tenant, String processInstanceId) {
        return store.findByProcessInstance(tenant, processInstanceId);
    }
}
