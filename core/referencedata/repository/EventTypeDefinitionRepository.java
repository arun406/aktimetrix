package com.aktimetrix.core.referencedata.repository;

import com.aktimetrix.core.referencedata.model.EventTypeDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "eventTypeDefinitions", path = "event-type-definitions")
public interface EventTypeDefinitionRepository extends MongoRepository<EventTypeDefinition, String> {
}
