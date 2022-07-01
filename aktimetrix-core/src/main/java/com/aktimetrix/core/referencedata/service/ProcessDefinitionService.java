package com.aktimetrix.core.referencedata.service;


import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.repository.ProcessDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    /**
     * returns the process definition of specified process Type and process code
     *
     * @return
     */
    public List<ProcessDefinition> get(String processType, String processCode) throws DefinitionNotFoundException {

        LookupOperation lookupOperation = LookupOperation.newLookup()
                .from("stepDefinitions")
                .localField("steps.stepCode")
                .foreignField("stepCode")
                .as("steps");
        Criteria criteria = null;
        if (StringUtils.hasText(processType) && StringUtils.hasText(processCode)) {
            criteria = Criteria.where("processType").is(processType)
                    .and("processCode").is(processCode);
        } else if (StringUtils.hasText(processType)) {
            criteria = Criteria.where("processType").is(processType);
        } else if (StringUtils.hasText(processCode)) {
            criteria = Criteria.where("processCode").is(processCode);
        }
        MatchOperation matchOperation = Aggregation.match(criteria);
        final Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupOperation);

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

        List<ProcessDefinition> definitions = mongoTemplate
                .aggregate(aggregation, "processDefinitions", ProcessDefinition.class)
                .getMappedResults();
        if (definitions.isEmpty()) {
            throw new DefinitionNotFoundException(String.format("No process definition found for process type %s, process code %s", processType, processCode));
        }
        return definitions;
    }
}
