package com.aktimetrix.core.referencedata.repository;

import com.aktimetrix.core.referencedata.model.MeasurementTypeDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MeasurementTypeDefinitionRepository extends MongoRepository<MeasurementTypeDefinition, String> {
}
