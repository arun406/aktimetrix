package com.aktimetrix.store.memory;

import com.aktimetrix.core.outbox.OutboxMessage;
import com.aktimetrix.core.store.OutboxStore;
import com.aktimetrix.core.store.StoreDocuments;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The outbox in memory. Claims are made under the store's lock, so they are atomic.
 */
final class MemoryOutboxStore implements OutboxStore {

    private final Map<String, OutboxMessage> messages = new LinkedHashMap<>();

    @Override
    public synchronized void add(OutboxMessage message) {
        if (message.getId() == null) {
            message.setId(MemoryInstanceStores.newId());
        }
        messages.put(message.getId(), StoreDocuments.copy(message));
    }

    @Override
    public synchronized Optional<OutboxMessage> claimNext(Instant now, Instant leaseUntil) {
        final Optional<OutboxMessage> next = messages.values().stream()
                .filter(m -> m.getSentAt() == null && (m.getLockedUntil() == null || m.getLockedUntil().isBefore(now)))
                .min(Comparator.comparing(OutboxMessage::getCreatedAt));
        next.ifPresent(m -> {
            m.setLockedUntil(leaseUntil);
            m.setAttempts(m.getAttempts() + 1);
        });
        return next.map(StoreDocuments::copy);
    }

    @Override
    public synchronized void markSent(String id, Instant sentAt) {
        final OutboxMessage message = messages.get(id);
        if (message != null) {
            message.setSentAt(sentAt);
            message.setLockedUntil(null);
        }
    }

    @Override
    public synchronized long countPending() {
        return messages.values().stream().filter(m -> m.getSentAt() == null).count();
    }

    @Override
    public synchronized long deleteSentBefore(Instant sentBefore) {
        final int before = messages.size();
        messages.values().removeIf(m -> m.getSentAt() != null && m.getSentAt().isBefore(sentBefore));
        return before - messages.size();
    }
}
