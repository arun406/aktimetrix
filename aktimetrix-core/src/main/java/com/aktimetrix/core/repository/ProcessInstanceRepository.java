package com.aktimetrix.core.repository;

import com.aktimetrix.core.model.ProcessInstance;
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

    @Query("{'tenant': ?0 , 'processCode' : ?1 , 'entityType' : ?2, 'entityId': ?3 }")
    List<ProcessInstance> findByTenantAndProcessCodeAndEntityTypeAndEntityId(String tenant, String processCode,
                                                                             String entityType, String entityId);

    @Query("{'tenant': ?0 , 'entityId': ?1 }")
    List<ProcessInstance> findByTenantAndEntityId(String tenant, String entityId);

    @Query("{'tenant': ?0 , 'entityType' : ?1, 'entityId': ?2 , 'complete': false }")
    List<ProcessInstance> findActiveByTenantAndEntityTypeAndEntityId(String tenant, String entityType, String entityId);
}
