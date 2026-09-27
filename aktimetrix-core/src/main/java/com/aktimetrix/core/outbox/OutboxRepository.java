package com.aktimetrix.core.outbox;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;

public interface OutboxRepository extends MongoRepository<OutboxMessage, ObjectId> {

    long countBySentAtIsNull();

    long deleteBySentAtBefore(Instant sentBefore);
}
