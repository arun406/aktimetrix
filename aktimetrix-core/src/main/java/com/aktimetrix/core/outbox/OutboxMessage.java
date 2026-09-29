package com.aktimetrix.core.outbox;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.Instant;

/**
 * An event waiting to be published to the message broker. Written in the same place as the state it describes, and sent by the
 * {@link OutboxRelay}, so a broker outage delays events instead of losing them.
 */
@Data
@NoArgsConstructor
public class OutboxMessage {
    @Id
    private String id;
    /**
     * Spring Cloud Stream binding to publish to, e.g. {@code step-instance-out-0}.
     */
    private String destination;
    /**
     * Message key: the id of the instance the event is about.
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
