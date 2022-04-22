package com.aktimetrix.core.referencedata.repository;

import com.aktimetrix.core.referencedata.model.EventTypeDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EventTypeDefinitionRepository extends MongoRepository<EventTypeDefinition, String> {

}
