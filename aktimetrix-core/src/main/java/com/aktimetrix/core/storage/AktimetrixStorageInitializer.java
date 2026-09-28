package com.aktimetrix.core.storage;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.outbox.OutboxMessage;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * Prepares the database before events are consumed.
 * <ul>
 *     <li>Upgrades process and step instances saved by earlier versions, which have no {@code revision} yet: without
 *     one, a save would be taken for an insert.</li>
 *     <li>Creates the indexes Aktimetrix's queries rely on. Creating an index that already exists does nothing.
 *     Disable with {@code aktimetrix.storage.create-indexes=false} to manage them yourself.</li>
 * </ul>
 * <p>
 * The process instance index is unique: at most one instance per tenant, process and entity, even when two events
 * start the same process at the same time.
 */
@Component
public class AktimetrixStorageInitializer implements SmartInitializingSingleton {
    private static final Logger logger = LoggerFactory.getLogger(AktimetrixStorageInitializer.class);

    private final MongoTemplate mongoTemplate;
    private final AktimetrixProperties properties;

    public AktimetrixStorageInitializer(MongoTemplate mongoTemplate, AktimetrixProperties properties) {
        this.mongoTemplate = mongoTemplate;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        try {
            upgradeRevisions();
            if (properties.getStorage().isCreateIndexes()) {
                createIndexes();
            }
        } catch (DataAccessResourceFailureException e) {
            // unreachable: do not wait for a timeout on every remaining call
            logger.error("MongoDB is unreachable; indexes were not checked: {}", e.getMessage());
        }
    }

    public void upgradeRevisions() {
        for (Class<?> type : new Class<?>[]{ProcessInstance.class, StepInstance.class}) {
            final long upgraded = mongoTemplate.updateMulti(Query.query(Criteria.where("revision").exists(false)),
                    new Update().set("revision", 0L), type).getModifiedCount();
            if (upgraded > 0) {
                logger.info("Added a revision to {} {} saved by an earlier version", upgraded,
                        mongoTemplate.getCollectionName(type));
            }
        }
    }

    public void createIndexes() {
        ensure(ProcessInstance.class, index("aktimetrix_process_entity", "tenant", "processCode", "entityType", "entityId").unique());
        ensure(ProcessInstance.class, index("aktimetrix_entity", "tenant", "entityId"));
        ensure(ProcessInstance.class, index("aktimetrix_process_deadlines", "lateAfter", "complete"));
        ensure(StepInstance.class, index("aktimetrix_process_steps", "tenant", "processInstanceId"));
        ensure(StepInstance.class, index("aktimetrix_deadlines", "lateAfter", "status"));
        ensure(MeasurementInstance.class, index("aktimetrix_process_measurements", "tenant", "processInstanceId"));
        ensure(OutboxMessage.class, index("aktimetrix_pending", "sentAt", "createdAt"));
        ensure(ProcessDefinition.class, index("aktimetrix_process_code", "tenant", "processCode").unique());
        ensure(ProcessDefinition.class, index("aktimetrix_start_events", "tenant", "startEventCodes"));
        ensure(StepDefinition.class, index("aktimetrix_step_code", "tenant", "stepCode").unique());
    }

    private static Index index(String name, String... keys) {
        final Index index = new Index().named(name);
        for (String key : keys) {
            index.on(key, Direction.ASC);
        }
        return index;
    }

    private void ensure(Class<?> type, Index index) {
        try {
            mongoTemplate.indexOps(type).ensureIndex(index);
        } catch (DataAccessResourceFailureException e) {
            throw e;
        } catch (RuntimeException e) {
            // e.g. existing duplicates prevent a unique index; the application still works, only less safely
            logger.error("Could not create index {} on {}: {}", index.getIndexOptions().get("name"),
                    mongoTemplate.getCollectionName(type), e.getMessage());
        }
    }
}
