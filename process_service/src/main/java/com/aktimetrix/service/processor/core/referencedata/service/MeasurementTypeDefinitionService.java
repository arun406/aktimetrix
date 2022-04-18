package com.aktimetrix.service.processor.core.referencedata.service;

import com.aktimetrix.service.processor.core.referencedata.model.MeasurementTypeDefinition;
import com.aktimetrix.service.processor.core.referencedata.repository.MeasurementTypeDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MeasurementTypeDefinitionService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MeasurementTypeDefinitionRepository repository;

    /**
     * @return
     */
    public List<MeasurementTypeDefinition> list() {

        LookupOperation lookupOperation = LookupOperation.newLookup()
                .from("measurementUnitDefinitions")
                .localField("unitCode")
                .foreignField("code")
                .as("unit");

        final Aggregation aggregation = Aggregation.newAggregation(lookupOperation, Aggregation.unwind("unit"));
        final AggregationResults<MeasurementTypeDefinition> measurementTypeDefinitions = mongoTemplate
                .aggregate(aggregation, "measurementTypeDefinitions", MeasurementTypeDefinition.class);
        final List<MeasurementTypeDefinition> list = measurementTypeDefinitions.getMappedResults();
       /* AggregateIterable<MeasurementTypeDefinition> iterable = repository.aggregate(Arrays.asList(
                new Document("$lookup", new Document("from", "measurementUnitDefinitions")
                        .append("localField", "unitCode")
                        .append("foreignField", "code")
                        .append("as", "unit")
                ),
                new Document("$unwind", "$unit")
        ));
        iterable.forEach(measurementTypeDefinition -> {
            logger.info(" printing: " + measurementTypeDefinition);
            list.add(measurementTypeDefinition);
        });*/

        return list;
    }


    /**
     * @param definition
     */
    public void add(MeasurementTypeDefinition definition) {
        this.repository.save(definition);
    }
}
