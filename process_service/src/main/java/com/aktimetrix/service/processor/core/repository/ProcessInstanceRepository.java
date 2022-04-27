package com.aktimetrix.service.processor.core.repository;

import com.aktimetrix.service.processor.core.model.ProcessInstance;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * @author arun kumar kandakatla
 */
public interface ProcessInstanceRepository extends MongoRepository<ProcessInstance, String> {

    /**
     * @param tenant
     * @param processInstanceId
     * @return
     */
    @Query("{'tenant': ?0 , '_id' : ?1 }")
    List<ProcessInstance> findByTenantAndId(String tenant, ObjectId processInstanceId);

    /**
     * @param tenant
     * @param processCode
     * @param entityType
     * @param entityId
     * @param status
     * @return
     */
    @Query("{'tenant': ?0 , 'processCode' : ?1 , 'entityType' : ?2, 'entityId': ?3 , 'status': ?4 }")
    ProcessInstance findByTenantAndProcessCodeAndEntityTypeAndEntityIdAndStatus(String tenant, String processCode,
                                                                                String entityType, String entityId,
                                                                                String status);
}
