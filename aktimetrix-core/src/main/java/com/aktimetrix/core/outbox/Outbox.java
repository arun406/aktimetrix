package com.aktimetrix.core.outbox;

import com.aktimetrix.core.service.AktimetrixMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Queues events for publishing to Kafka. See {@link OutboxRelay}.
 */
@Component
public class Outbox {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public Outbox(OutboxRepository repository, ObjectMapper objectMapper, Clock clock, AktimetrixMetrics metrics) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        metrics.outboxPending(repository::countBySentAtIsNull);
    }

    /**
     * Queues the event for the binding, with the Kafka message key.
     */
    public void enqueue(String destination, String messageKey, Object event) {
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Event cannot be serialized to JSON: " + event, e);
        }
        repository.save(new OutboxMessage(destination, messageKey, payload, clock.instant()));
    }
}
