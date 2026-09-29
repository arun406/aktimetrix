package com.aktimetrix.store.mongodb;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.store.MeasurementInstanceStore;
import com.aktimetrix.core.store.ProcessInstanceStore;
import com.aktimetrix.core.store.StepInstanceStore;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.aktimetrix.store.mongodb.MongoCollections.MEASUREMENT_INSTANCES;
import static com.aktimetrix.store.mongodb.MongoCollections.PROCESS_INSTANCES;
import static com.aktimetrix.store.mongodb.MongoCollections.STEP_INSTANCES;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Process, step and measurement instances in MongoDB. Saves are version-checked by Spring Data MongoDB, on the
 * {@code revision} field.
 */
final class MongoInstanceStores {

    private MongoInstanceStores() {
    }

    static final class Processes implements ProcessInstanceStore {
        private final MongoTemplate mongo;

        Processes(MongoTemplate mongo) {
            this.mongo = mongo;
        }

        @Override
        public ProcessInstance save(ProcessInstance instance) {
            return mongo.save(instance, PROCESS_INSTANCES);
        }

        @Override
        public Optional<ProcessInstance> findById(String tenant, String id) {
            return one(where("tenant").is(tenant).and("_id").is(id));
        }

        @Override
        public Optional<ProcessInstance> findByEntity(String tenant, String processCode, String entityType,
                                                      String entityId) {
            return one(where("tenant").is(tenant).and("processCode").is(processCode).and("entityType").is(entityType)
                    .and("entityId").is(entityId));
        }

        @Override
        public List<ProcessInstance> findByEntityId(String tenant, String entityId) {
            return all(where("tenant").is(tenant).and("entityId").is(entityId));
        }

        @Override
        public List<ProcessInstance> findNotCancelled(String tenant, String entityType, String entityId) {
            return all(where("tenant").is(tenant).and("entityType").is(entityType).and("entityId").is(entityId)
                    .and("status").ne(Constants.STATUS_CANCELLED));
        }

        @Override
        public List<ProcessInstance> findOverdue(LocalDateTime now) {
            return all(where("complete").is(false).and("lateAfter").lt(now).and("timeliness").ne(Timeliness.OVERDUE.name()));
        }

        private Optional<ProcessInstance> one(Criteria criteria) {
            return Optional.ofNullable(mongo.findOne(Query.query(criteria), ProcessInstance.class, PROCESS_INSTANCES));
        }

        private List<ProcessInstance> all(Criteria criteria) {
            return mongo.find(Query.query(criteria), ProcessInstance.class, PROCESS_INSTANCES);
        }
    }

    static final class Steps implements StepInstanceStore {
        private final MongoTemplate mongo;

        Steps(MongoTemplate mongo) {
            this.mongo = mongo;
        }

        @Override
        public StepInstance save(StepInstance step) {
            return mongo.save(step, STEP_INSTANCES);
        }

        @Override
        public Optional<StepInstance> findById(String id) {
            return Optional.ofNullable(mongo.findById(id, StepInstance.class, STEP_INSTANCES));
        }

        @Override
        public List<StepInstance> findByProcessInstance(String tenant, String processInstanceId) {
            return mongo.find(Query.query(where("tenant").is(tenant).and("processInstanceId").is(processInstanceId))
                    .with(Sort.by("sequence")), StepInstance.class, STEP_INSTANCES);
        }

        @Override
        public List<StepInstance> findOverdue(LocalDateTime now) {
            return mongo.find(Query.query(where("status").nin(Constants.STATUS_COMPLETED, Constants.STATUS_CANCELLED,
                            Constants.STATUS_SKIPPED).and("lateAfter").lt(now).and("timeliness").ne(Timeliness.OVERDUE.name())),
                    StepInstance.class, STEP_INSTANCES);
        }
    }

    static final class Measurements implements MeasurementInstanceStore {
        private final MongoTemplate mongo;

        Measurements(MongoTemplate mongo) {
            this.mongo = mongo;
        }

        @Override
        public MeasurementInstance save(MeasurementInstance measurement) {
            return mongo.save(measurement, MEASUREMENT_INSTANCES);
        }

        @Override
        public List<MeasurementInstance> findByProcessInstance(String tenant, String processInstanceId) {
            return mongo.find(Query.query(where("tenant").is(tenant).and("processInstanceId").is(processInstanceId)),
                    MeasurementInstance.class, MEASUREMENT_INSTANCES);
        }

        @Override
        public List<MeasurementInstance> find(String tenant, String processInstanceId, String stepInstanceId,
                                              String code, String type) {
            return mongo.find(Query.query(where("tenant").is(tenant).and("processInstanceId").is(processInstanceId)
                            .and("stepInstanceId").is(stepInstanceId).and("code").is(code).and("type").is(type)),
                    MeasurementInstance.class, MEASUREMENT_INSTANCES);
        }
    }
}
