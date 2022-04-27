package com.aktimetrix.service.meter.referencedata.repository;

import com.aktimetrix.service.meter.referencedata.model.ProcessDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessDefinitionRepository extends MongoRepository<ProcessDefinition, String> {
}
