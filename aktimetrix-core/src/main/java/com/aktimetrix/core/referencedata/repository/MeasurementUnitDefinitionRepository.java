package com.aktimetrix.core.referencedata.repository;

import com.aktimetrix.core.referencedata.model.MeasurementUnitDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeasurementUnitDefinitionRepository extends MongoRepository<MeasurementUnitDefinition, String> {

}
