package com.aktimetrix.core.referencedata.service;


import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.repository.ProcessDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProcessDefinitionService {

    @Autowired
    ProcessDefinitionRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

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

        /* AggregateIterable<ProcessDefinition> aggregate = this.repository.aggregate(Arrays.asList(
                new Document("$lookup", new Document("from", "stepDefinitions")
                        .append("localField", "steps.stepCode")
                        .append("foreignField", "stepCode")
                        .append("as", "steps")
                )
        ));
        aggregate.forEach(definition -> {
            processDefinitions.add(definition);
        });*/

        return mongoTemplate
                .aggregate(aggregation, "processDefinitions", ProcessDefinition.class)
                .getMappedResults();
    }
}
