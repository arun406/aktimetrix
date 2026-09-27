package com.aktimetrix.core.outbox;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * An event waiting to be published to Kafka. Written in the same place as the state it describes, and sent by the
 * {@link OutboxRelay}, so a Kafka outage delays events instead of losing them.
 */
@Data
@NoArgsConstructor
@Document(collection = "outbox")
public class OutboxMessage {
    @Id
    private ObjectId id;
    /**
     * Spring Cloud Stream binding to publish to, e.g. {@code step-instance-out-0}.
     */
    private String destination;
    /**
     * Kafka message key.
     */
    private String messageKey;
    /**
     * The event, as JSON.
     */
    private String payload;
    private Instant createdAt;
    /**
     * When the relay published the message; {@code null} while pending.
     */
    private Instant sentAt;
    /**
     * Until when a relay has claimed the message.
     */
    private Instant lockedUntil;
    private int attempts;

    public OutboxMessage(String destination, String messageKey, String payload, Instant createdAt) {
        this.destination = destination;
        this.messageKey = messageKey;
        this.payload = payload;
        this.createdAt = createdAt;
    }
}
