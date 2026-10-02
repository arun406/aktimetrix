package com.aktimetrix.store.memory;

import com.aktimetrix.core.store.ProcessedEventStore;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Processed events in memory.
 */
final class MemoryProcessedEventStore implements ProcessedEventStore {

    private final Map<String, Instant> processed = new HashMap<>();

    @Override
    public synchronized boolean isProcessed(String tenant, String eventId) {
        return processed.containsKey(key(tenant, eventId));
    }

    @Override
    public synchronized void markProcessed(String tenant, String eventId, Instant processedAt) {
        if (processed.putIfAbsent(key(tenant, eventId), processedAt) != null) {
            throw new DuplicateKeyException("Event " + eventId + " of tenant " + tenant + " was already processed");
        }
    }

    @Override
    public synchronized long deleteProcessedBefore(Instant before) {
        final int size = processed.size();
        processed.values().removeIf(at -> at.isBefore(before));
        return size - processed.size();
    }

    private static String key(String tenant, String eventId) {
        return tenant + '\u0000' + eventId;
    }
}
