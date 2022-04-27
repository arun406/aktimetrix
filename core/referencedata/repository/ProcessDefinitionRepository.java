package com.aktimetrix.core.referencedata.repository;

import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessDefinitionRepository extends MongoRepository<ProcessDefinition, String> {
}
