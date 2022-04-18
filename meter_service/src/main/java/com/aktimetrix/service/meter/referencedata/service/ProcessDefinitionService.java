package com.aktimetrix.service.meter.referencedata.service;


import com.aktimetrix.service.meter.referencedata.model.ProcessDefinition;
import com.aktimetrix.service.meter.referencedata.repository.ProcessDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessDefinitionService {

    private final ProcessDefinitionRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Saves the Process Definition
     *
     * @param definition
     * @return
     */
    public ProcessDefinition add(ProcessDefinition definition) {
        this.repository.save(definition);
        return definition;
    }

    /**
     * returns the collection of process definitions
     *
     * @return
     */
    public List<ProcessDefinition> list() {

        LookupOperation lookupOperation = LookupOperation.newLookup()
                .from("stepDefinitions")
                .localField("steps.stepCode")
                .foreignField("stepCode")
                .as("steps");
        final Aggregation aggregation = Aggregation.newAggregation(lookupOperation);

        return mongoTemplate
                .aggregate(aggregation, "processDefinitions", ProcessDefinition.class)
                .getMappedResults();
    }
}
