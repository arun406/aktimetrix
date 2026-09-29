package com.aktimetrix.store.mongodb;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static com.aktimetrix.store.mongodb.MongoCollections.ALARMS;
import static com.aktimetrix.store.mongodb.MongoCollections.MEASUREMENT_INSTANCES;
import static com.aktimetrix.store.mongodb.MongoCollections.OUTBOX;
import static com.aktimetrix.store.mongodb.MongoCollections.PROCESS_DEFINITIONS;
import static com.aktimetrix.store.mongodb.MongoCollections.PROCESS_INSTANCES;
import static com.aktimetrix.store.mongodb.MongoCollections.STEP_DEFINITIONS;
import static com.aktimetrix.store.mongodb.MongoCollections.STEP_INSTANCES;

/**
 * Prepares the database before the stores are used.
 * <ul>
 *     <li>Upgrades instances saved by earlier versions: adds a {@code revision} to process and step instances that
 *     have none (without one, a save would be taken for an insert), and stores references to process and step
 *     instances as strings, as they are now, rather than as object ids.</li>
 *     <li>Creates the indexes Aktimetrix's queries rely on. Creating an index that already exists does nothing.
 *     Disable with {@code aktimetrix.storage.create-indexes=false} to manage them yourself.</li>
 * </ul>
 * <p>
 * The process instance index is unique: at most one instance per tenant, process and entity, even when two events
 * start the same process at the same time.
 */
public class MongoStoreInitializer implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(MongoStoreInitializer.class);

    private final MongoTemplate mongoTemplate;
    private final AktimetrixProperties properties;

    public MongoStoreInitializer(MongoTemplate mongoTemplate, AktimetrixProperties properties) {
        this.mongoTemplate = mongoTemplate;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            upgrade();
            if (properties.getStorage().isCreateIndexes()) {
                createIndexes();
            }
        } catch (DataAccessResourceFailureException e) {
            // unreachable: do not wait for a timeout on every remaining call
            logger.error("MongoDB is unreachable; indexes were not checked: {}", e.getMessage());
        }
    }

    public void upgrade() {
        for (String collection : new String[]{PROCESS_INSTANCES, STEP_INSTANCES}) {
            final long upgraded = mongoTemplate.updateMulti(Query.query(Criteria.where("revision").exists(false)),
                    new Update().set("revision", 0L), collection).getModifiedCount();
            if (upgraded > 0) {
                logger.info("Added a revision to {} {} saved by an earlier version", upgraded, collection);
            }
        }
        referencesAsStrings(STEP_INSTANCES, "processInstanceId");
        referencesAsStrings(MEASUREMENT_INSTANCES, "processInstanceId");
        referencesAsStrings(MEASUREMENT_INSTANCES, "stepInstanceId");
    }

    private void referencesAsStrings(String collection, String field) {
        final MongoCollection<Document> documents = mongoTemplate.getCollection(collection);
        long upgraded = 0;
        for (Document document : documents.find(Filters.type(field, BsonType.OBJECT_ID))) {
            documents.updateOne(Filters.eq("_id", document.get("_id")),
                    Updates.set(field, document.get(field, ObjectId.class).toHexString()));
            upgraded++;
        }
        if (upgraded > 0) {
            logger.info("Stored {} of {} {} as a string", field, upgraded, collection);
        }
    }

    public void createIndexes() {
        ensure(PROCESS_INSTANCES, index("aktimetrix_process_entity", "tenant", "processCode", "entityType", "entityId").unique());
        ensure(PROCESS_INSTANCES, index("aktimetrix_entity", "tenant", "entityId"));
        ensure(PROCESS_INSTANCES, index("aktimetrix_process_deadlines", "lateAfter", "complete"));
        ensure(STEP_INSTANCES, index("aktimetrix_process_steps", "tenant", "processInstanceId"));
        ensure(STEP_INSTANCES, index("aktimetrix_deadlines", "lateAfter", "status"));
        ensure(MEASUREMENT_INSTANCES, index("aktimetrix_process_measurements", "tenant", "processInstanceId"));
        ensure(OUTBOX, index("aktimetrix_pending", "sentAt", "createdAt"));
        ensure(ALARMS, index("aktimetrix_due", "dueAt"));
        ensure(PROCESS_DEFINITIONS, index("aktimetrix_process_code", "tenant", "processCode").unique());
        ensure(PROCESS_DEFINITIONS, index("aktimetrix_start_events", "tenant", "startEventCodes"));
        ensure(STEP_DEFINITIONS, index("aktimetrix_step_code", "tenant", "stepCode").unique());
    }

    private static Index index(String name, String... keys) {
        final Index index = new Index().named(name);
        for (String key : keys) {
            index.on(key, Direction.ASC);
        }
        return index;
    }

    private void ensure(String collection, Index index) {
        try {
            mongoTemplate.indexOps(collection).ensureIndex(index);
        } catch (DataAccessResourceFailureException e) {
            throw e;
        } catch (RuntimeException e) {
            // e.g. existing duplicates prevent a unique index; the application still works, only less safely
            logger.error("Could not create index {} on {}: {}", index.getIndexOptions().get("name"), collection,
                    e.getMessage());
        }
    }
}
