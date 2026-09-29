package com.aktimetrix.store.mongodb;

import com.aktimetrix.core.referencedata.model.MeasurementTypeDefinition;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.store.DefinitionStore;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

import static com.aktimetrix.store.mongodb.MongoCollections.MEASUREMENT_TYPE_DEFINITIONS;
import static com.aktimetrix.store.mongodb.MongoCollections.PROCESS_DEFINITIONS;
import static com.aktimetrix.store.mongodb.MongoCollections.STEP_DEFINITIONS;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Definitions in MongoDB, one document per definition.
 */
final class MongoDefinitionStore implements DefinitionStore {

    private final MongoTemplate mongo;

    MongoDefinitionStore(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public ProcessDefinition saveProcess(ProcessDefinition definition) {
        if (definition.getId() == null) {
            findProcess(definition.getTenant(), definition.getProcessCode()).ifPresent(e -> definition.setId(e.getId()));
        }
        return mongo.save(definition, PROCESS_DEFINITIONS);
    }

    @Override
    public Optional<ProcessDefinition> findProcess(String tenant, String processCode) {
        return Optional.ofNullable(mongo.findOne(Query.query(where("tenant").is(tenant).and("processCode").is(processCode)),
                ProcessDefinition.class, PROCESS_DEFINITIONS));
    }

    @Override
    public List<ProcessDefinition> findProcesses() {
        return mongo.findAll(ProcessDefinition.class, PROCESS_DEFINITIONS);
    }

    @Override
    public List<ProcessDefinition> findConfirmedProcessesStartedBy(String tenant, String eventCode) {
        return mongo.find(Query.query(where("tenant").is(tenant).and("status").is("CONFIRMED")
                .and("startEventCodes").is(eventCode)), ProcessDefinition.class, PROCESS_DEFINITIONS);
    }

    @Override
    public StepDefinition saveStep(StepDefinition definition) {
        if (definition.getId() == null) {
            findStep(definition.getTenant(), definition.getStepCode()).ifPresent(e -> definition.setId(e.getId()));
        }
        return mongo.save(definition, STEP_DEFINITIONS);
    }

    @Override
    public Optional<StepDefinition> findStep(String tenant, String stepCode) {
        return Optional.ofNullable(mongo.findOne(Query.query(where("tenant").is(tenant).and("stepCode").is(stepCode)),
                StepDefinition.class, STEP_DEFINITIONS));
    }

    @Override
    public List<StepDefinition> findSteps() {
        return mongo.findAll(StepDefinition.class, STEP_DEFINITIONS);
    }

    @Override
    public MeasurementTypeDefinition saveMeasurementType(MeasurementTypeDefinition definition) {
        return mongo.save(definition, MEASUREMENT_TYPE_DEFINITIONS);
    }

    @Override
    public List<MeasurementTypeDefinition> findMeasurementTypes() {
        return mongo.findAll(MeasurementTypeDefinition.class, MEASUREMENT_TYPE_DEFINITIONS);
    }
}
