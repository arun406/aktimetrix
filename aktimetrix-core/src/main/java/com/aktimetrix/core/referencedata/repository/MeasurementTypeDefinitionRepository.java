package com.aktimetrix.core.referencedata.repository;

import com.aktimetrix.core.referencedata.model.MeasurementTypeDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "measurement-type-definitions", itemResourceRel = "measurement-type", path = "measurement-type-definitions")
public interface MeasurementTypeDefinitionRepository extends MongoRepository<MeasurementTypeDefinition, String> {
}
