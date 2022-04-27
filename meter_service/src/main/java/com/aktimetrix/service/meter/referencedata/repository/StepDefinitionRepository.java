package com.aktimetrix.service.meter.referencedata.repository;

import com.aktimetrix.service.meter.referencedata.model.StepDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 *
 */
public interface StepDefinitionRepository extends MongoRepository<StepDefinition, String> {

    /**
     * @param tenant
     * @param stepCode
     * @return
     */
    @Query("{ 'tenant': ?0, 'stepCode' : ?1 }")
    StepDefinition findByStepCode(String tenant, String stepCode);
}
