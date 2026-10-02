package com.aktimetrix.core.store;

import java.time.Instant;

/**
 * Remembers the business events already processed, by tenant and {@code eventId}, so that a duplicate, such as a
 * message the broker delivers twice or a source system sends again, is recognised and ignored.
 * <p>
 * {@link #markProcessed} is called in the same unit of work as the event's processing: with an atomic store, an event
 * whose processing fails is not marked, and its retry is processed.
 */
public interface ProcessedEventStore {

    /**
     * Whether the event was already processed.
     */
    boolean isProcessed(String tenant, String eventId);

    /**
     * Records the event as processed.
     *
     * @throws org.springframework.dao.DuplicateKeyException when it already was: another instance processed it at the
     *                                                       same time
     */
    void markProcessed(String tenant, String eventId, Instant processedAt);

    /**
     * Forgets events processed before the given time.
     *
     * @return how many were forgotten
     */
    long deleteProcessedBefore(Instant before);
}
