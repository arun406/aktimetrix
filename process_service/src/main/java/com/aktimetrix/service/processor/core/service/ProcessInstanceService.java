package com.aktimetrix.service.processor.core.service;

import com.aktimetrix.service.processor.core.api.Registry;
import com.aktimetrix.service.processor.core.model.ProcessInstance;
import com.aktimetrix.service.processor.core.repository.ProcessInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessInstanceService.class);

    private final ProcessInstanceRepository repository;
    private final Registry registry;

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
        return this.repository
                .findByTenantAndProcessCodeAndEntityTypeAndEntityIdAndStatus(tenant, processCode, entityType, entityId, "Created");
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
