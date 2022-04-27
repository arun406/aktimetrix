package com.aktimetrix.service.processor.core.referencedata.repository;

import com.aktimetrix.service.processor.core.referencedata.model.EventTypeDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EventTypeDefinitionRepository extends MongoRepository<EventTypeDefinition, String> {

}
