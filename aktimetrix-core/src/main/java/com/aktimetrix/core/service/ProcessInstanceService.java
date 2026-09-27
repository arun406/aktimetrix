package com.aktimetrix.core.service;

import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.repository.ProcessInstanceRepository;
import com.aktimetrix.core.repository.StepInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessInstanceService.class);

    private final ProcessInstanceRepository repository;
    private final StepInstanceRepository stepInstanceRepository;

    /**
     * saves the process instance object to database.
     *
     * @param processInstance process instance to be saved
     * @return saved process instance
     */
    public ProcessInstance saveProcessInstance(ProcessInstance processInstance) {
        // Save Process Instance
        this.repository.save(processInstance);
        logger.info("Process Instance Id :" + processInstance.getId());
        return processInstance;
    }

    /**
     * Returns the process instance
     *
     * @param tenant      tenant
     * @param processCode process code
     * @param entityType  entity type
     * @param entityId    entity id
     * @return process instance
     */
    public ProcessInstance getProcessInstance(String tenant, String processCode, String entityType, String entityId) {
        // not filtered by status, so a completed process is not re-created when its start event is replayed
        return this.repository
                .findByTenantAndProcessCodeAndEntityTypeAndEntityId(tenant, processCode, entityType, entityId)
                .stream().findFirst().orElse(null);
    }

    /**
     * Returns every process instance of the business entity, with its step instances.
     *
     * @param entityType entity type to match, or {@code null} for any
     */
    public List<ProcessInstance> getProcessInstancesWithSteps(String tenant, String entityType, String entityId) {
        final List<ProcessInstance> instances = this.repository.findByTenantAndEntityId(tenant, entityId).stream()
                .filter(instance -> entityType == null || entityType.equals(instance.getEntityType()))
                .collect(Collectors.toList());
        instances.forEach(instance -> instance.setSteps(stepInstanceRepository.findByTenantAndProcessInstanceId(tenant, instance.getId())));
        return instances;
    }

    /**
     * Returns the process instances of the given business entity that are not complete yet.
     */
    public List<ProcessInstance> getActiveProcessInstances(String tenant, String entityType, String entityId) {
        return this.repository.findActiveByTenantAndEntityTypeAndEntityId(tenant, entityType, entityId);
    }


    /**
     * Returns the ProcessInstance By id
     *
     * @param tenant            tenant
     * @param processInstanceId process instance reference
     * @return process instance
     */
    public ProcessInstance getProcessInstance(String tenant, ObjectId processInstanceId) {
        return this.repository.findByTenantAndId(tenant, processInstanceId).stream().findFirst().orElse(null);
    }
}
