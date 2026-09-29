package com.aktimetrix.store.mongodb;

import com.aktimetrix.core.outbox.OutboxMessage;
import com.aktimetrix.core.store.OutboxStore;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.Optional;

import static com.aktimetrix.store.mongodb.MongoCollections.OUTBOX;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * The outbox in MongoDB. A message is claimed with an atomic find-and-modify.
 */
final class MongoOutboxStore implements OutboxStore {

    private final MongoTemplate mongo;

    MongoOutboxStore(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void add(OutboxMessage message) {
        mongo.insert(message, OUTBOX);
    }

    @Override
    public Optional<OutboxMessage> claimNext(Instant now, Instant leaseUntil) {
        final Query pending = Query.query(where("sentAt").is(null).orOperator(
                        where("lockedUntil").is(null), where("lockedUntil").lt(now)))
                .with(Sort.by("createdAt", "_id"));
        final Update claim = new Update().set("lockedUntil", leaseUntil).inc("attempts", 1);
        return Optional.ofNullable(mongo.findAndModify(pending, claim, FindAndModifyOptions.options().returnNew(true),
                OutboxMessage.class, OUTBOX));
    }

    @Override
    public void markSent(String id, Instant sentAt) {
        mongo.updateFirst(Query.query(where("_id").is(id)), new Update().set("sentAt", sentAt).unset("lockedUntil"),
                OutboxMessage.class, OUTBOX);
    }

    @Override
    public long countPending() {
        return mongo.count(Query.query(where("sentAt").is(null)), OUTBOX);
    }

    @Override
    public long deleteSentBefore(Instant sentBefore) {
        return mongo.remove(Query.query(Criteria.where("sentAt").lt(sentBefore)), OUTBOX).getDeletedCount();
    }
}
