package com.aktimetrix.core.referencedata.repository;

import com.aktimetrix.core.referencedata.model.StepDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 *
 */
@RepositoryRestResource(itemResourceRel = "step-definition", collectionResourceRel = "step-definitions", path = "step-definitions")
public interface StepDefinitionRepository extends MongoRepository<StepDefinition, String> {

    /**
     * @param tenant
     * @param stepCode
     * @return
     */
    @Query("{ 'tenant': ?0, 'stepCode' : ?1 , 'status': ?2}")
    StepDefinition findByStepCodeAndStatus(String tenant, String stepCode, String status);

    @Query("{ 'tenant': ?0, 'startEventCodes': ?1, 'status' : 'CONFIRMED'}")
    List<StepDefinition> findByStartEventCode(String tenant, String eventCode);
}
