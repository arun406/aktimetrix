package com.aktimetrix.core.referencedata.repository;

import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProcessDefinitionRepository extends MongoRepository<ProcessDefinition, String> {

    List<ProcessDefinition> findByTenantAndProcessCode(@Param("tenant") String tenant, @Param("codes") String codes);

    List<ProcessDefinition> findByProcessCode(@Param("codes") String codes);

    /**
     * Confirmed process definitions of the tenant that the event code starts.
     */
    @Query("{ 'tenant': ?0, 'status': 'CONFIRMED', 'startEventCodes': ?1 }")
    List<ProcessDefinition> findConfirmedStartedBy(String tenant, String eventCode);
}
