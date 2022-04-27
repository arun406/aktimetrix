package com.aktimetrix.core.referencedata.repository;

import com.aktimetrix.core.referencedata.model.StepDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

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
    @Query("{ 'tenant': ?0, 'stepCode' : ?1 }")
    StepDefinition findByStepCode(String tenant, String stepCode);
}
