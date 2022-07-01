package com.aktimetrix.core.repository;

import com.aktimetrix.core.model.ProcessPlanInstance;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * @author arun kumar kandakatla
 */
public interface ProcessPlanRepository extends MongoRepository<ProcessPlanInstance, String> {

    @Query("{ 'tenant' : ?0 , 'processInstanceId': ?1, 'activeInd': ?2 }")
    ProcessPlanInstance findPlanByProcessInstanceIdAndActiveIndicator(String tenant, String id, String activeIndicator);

    @Query("{'tenant' : ?0 , 'entityType': ?1, 'entityId': ?2 , 'status' : {$nin : ['Cancelled','Created']}}")
    List<ProcessPlanInstance> findPlanByEntityIdAndEntityType(String tenant, String entityId, String entityType, PageRequest pageRequest);

    @Query("{ 'tenant' : ?0 , 'processInstanceId': ?1, 'completeInd': ?2 }")
    ProcessPlanInstance findPlanByProcessInstanceIdAndCompleteIndicator(String tenant, String processInstanceId, String completeIndicator);

    @Query("{'tenant' : ?0 , 'processInstanceId': ?1}")
    List<ProcessPlanInstance> findPlanByProcessInstanceId(String tenant, String id);
}
