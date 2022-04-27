package com.aktimetrix.service.processor.core.referencedata.repository;

import com.aktimetrix.service.processor.core.referencedata.model.ProcessDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessDefinitionRepository extends MongoRepository<ProcessDefinition, String> {
}
