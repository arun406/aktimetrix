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
     * @param definitions
     */
    public void add(List<ProcessDefinition> definitions) {
        this.repository.saveAll(definitions);
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

        return this.mongoTemplate
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

        List<ProcessDefinition> definitions = mongoTemplate
                .aggregate(aggregation, "processDefinitions", ProcessDefinition.class)
                .getMappedResults();
        if (definitions.isEmpty()) {
            throw new DefinitionNotFoundException(String.format("No process definition found for process type %s, process code %s", processType, processCode));
        }
        return definitions;
    }

    /**
     * Returns Process Definitions by start event code
     *
     * @param tenant
     * @param eventCode
     * @return
     */
    public List<ProcessDefinition> getByEventCode(String tenant, String eventCode) {
        LookupOperation lookupOperation = LookupOperation.newLookup()
                .from("stepDefinitions")
                .localField("steps.stepCode")
                .foreignField("stepCode")
                .as("steps");
        final Criteria criteria = Criteria.where("tenant").is(tenant).and("startEventCodes").is(eventCode);
        MatchOperation matchOperation = Aggregation.match(criteria);
        final Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupOperation);

        List<ProcessDefinition> definitions = mongoTemplate
                .aggregate(aggregation, "processDefinitions", ProcessDefinition.class)
                .getMappedResults();

        return definitions;
    }
}
