package com.aktimetrix.core.outbox;

import com.aktimetrix.core.service.AktimetrixMetrics;
import com.aktimetrix.core.store.OutboxStore;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Queues events for publishing to the message broker, in the state store, in the same unit of work as the state they
 * describe. See {@link OutboxRelay}.
 */
@Component
public class Outbox {

    private final OutboxStore store;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public Outbox(OutboxStore store, ObjectMapper objectMapper, Clock clock, AktimetrixMetrics metrics) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.clock = clock;
        metrics.outboxPending(store::countPending);
    }

    /**
     * Queues the event for the binding, with its message key.
     */
    public void enqueue(String destination, String messageKey, Object event) {
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Event cannot be serialized to JSON: " + event, e);
        }
        enqueueRaw(destination, messageKey, payload);
    }

    /**
     * Queues a payload that is already serialized, as is, for the binding.
     */
    public void enqueueRaw(String destination, String messageKey, String payload) {
        store.add(new OutboxMessage(destination, messageKey, payload, clock.instant()));
    }
}
