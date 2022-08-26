package com.aktimetrix.core.service;

import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.repository.ProcessInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessInstanceService.class);

    private final ProcessInstanceRepository repository;

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
    public Page<ProcessInstance> getProcessInstance(String tenant, String processType, String processCode, String entityType, String entityId) {
        Sort sort = Sort.by(Sort.Direction.DESC, "version");
        PageRequest pageable = PageRequest.of(0, 1, sort);
        return this.repository
                .findByTenantAndProcessCodeAndProcessTypeAndEntityTypeAndEntityId(tenant, processType, processCode, entityType, entityId, pageable);
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
    public List<ProcessInstance> getProcessInstances(String tenant, String processType, String processCode, String entityType, String entityId) {
        return this.repository
                .findByTenantAndProcessCodeAndProcessTypeAndEntityTypeAndEntityId(tenant, processType, processCode, entityType, entityId);
    }

    /**
     * Returns the ProcessInstance By id
     *
     * @param tenant            tenant
     * @param processInstanceId process instance reference
     * @return process instance
     */
    public ProcessInstance getProcessInstance(String tenant, String processInstanceId) {
        return this.repository.findByTenantAndId(tenant, new ObjectId(processInstanceId)).stream().findFirst().orElse(null);
    }


    /**
     * set the process instance status to 'Cancelled'
     *
     * @param tenant
     * @param processInstanceId
     * @param cancellationReason
     */
    public void cancelProcessInstance(String tenant, String processInstanceId, String cancellationReason) {
        ProcessInstance processInstance = this.repository.findById(processInstanceId).orElseThrow(() -> new RuntimeException("process instance not found"));
        processInstance.setStatus("Cancelled");
        processInstance.setCancellationReason(cancellationReason);
        processInstance.setModifiedOn(LocalDateTime.now());
        this.repository.save(processInstance);
    }
}
