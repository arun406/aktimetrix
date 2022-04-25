package com.aktimetrix.core.referencedata.repository;

import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

@RepositoryRestResource(itemResourceRel = "process-definition", collectionResourceRel = "process-definitions", path = "process-definitions")
public interface ProcessDefinitionRepository extends MongoRepository<ProcessDefinition, String> {

    List<ProcessDefinition> findByTenantAndProcessCode(@Param("tenant") String tenant, @Param("codes") String codes);

    List<ProcessDefinition> findByProcessCode(@Param("codes") String codes);

}
