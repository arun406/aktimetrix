package com.aktimetrix.service.processor.core.referencedata.repository;

import com.aktimetrix.service.processor.core.referencedata.model.MeasurementTypeDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MeasurementTypeDefinitionRepository extends MongoRepository<MeasurementTypeDefinition, String> {
}
