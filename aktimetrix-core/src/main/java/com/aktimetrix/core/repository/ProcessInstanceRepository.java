package com.aktimetrix.core.repository;

import com.aktimetrix.core.model.ProcessInstance;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Query("{'tenant': ?0 , 'processType' : ?1, 'processCode' : ?2 , 'entityType' : ?3, 'entityId': ?4 , 'status': ?5 }")
    ProcessInstance findByTenantAndProcessCodeAndProcessTypeAndEntityTypeAndEntityIdAndStatus(String tenant, String processType, String processCode,
                                                                                              String entityType, String entityId,
                                                                                              String status);

    /**
     * @param tenant
     * @param processType
     * @param processCode
     * @param entityType
     * @param entityId
     * @return
     */
    @Query("{'tenant': ?0 , 'processType' : ?1, 'processCode' : ?2 , 'entityType' : ?3, 'entityId': ?4, status: {$ne: 'Cancelled'}}")
    Page<ProcessInstance> findByTenantAndProcessCodeAndProcessTypeAndEntityTypeAndEntityId(String tenant,
                                                                                           String processType, String processCode, String entityType, String entityId, Pageable pageable);

    /**
     * @param tenant
     * @param processType
     * @param processCode
     * @param entityType
     * @param entityId
     * @return
     */
    @Query("{'tenant': ?0 , 'processType' : ?1, 'processCode' : ?2 , 'entityType' : ?3, 'entityId': ?4, status: {$ne: 'Cancelled'}}")
    List<ProcessInstance> findByTenantAndProcessCodeAndProcessTypeAndEntityTypeAndEntityId(String tenant,
                                                                                           String processType, String processCode, String entityType, String entityId);
}
