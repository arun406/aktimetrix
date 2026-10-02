package com.aktimetrix.store.mongodb;

import com.aktimetrix.core.store.ProcessedEventStore;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.Date;

import static com.aktimetrix.store.mongodb.MongoCollections.PROCESSED_EVENTS;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Processed events in a collection whose {@code _id} is the tenant and event id: a second insert of the same event
 * fails with a {@link org.springframework.dao.DuplicateKeyException}.
 */
final class MongoProcessedEventStore implements ProcessedEventStore {

    private final MongoTemplate mongo;

    MongoProcessedEventStore(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public boolean isProcessed(String tenant, String eventId) {
        return mongo.exists(Query.query(where("_id").is(id(tenant, eventId))), PROCESSED_EVENTS);
    }

    @Override
    public void markProcessed(String tenant, String eventId, Instant processedAt) {
        mongo.insert(new Document("_id", id(tenant, eventId)).append("tenant", tenant).append("eventId", eventId)
                .append("processedAt", Date.from(processedAt)), PROCESSED_EVENTS);
    }

    @Override
    public long deleteProcessedBefore(Instant before) {
        return mongo.remove(Query.query(where("processedAt").lt(Date.from(before))), PROCESSED_EVENTS).getDeletedCount();
    }

    private static Document id(String tenant, String eventId) {
        return new Document("tenant", tenant).append("eventId", eventId);
    }
}
