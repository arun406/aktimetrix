package com.aktimetrix.store.memory;

import com.aktimetrix.core.model.Alarm;
import com.aktimetrix.core.store.AlarmStore;
import com.aktimetrix.core.store.StoreDocuments;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Alarms in memory. Claims are made under the store's lock, so they are atomic.
 */
final class MemoryAlarmStore implements AlarmStore {

    private final Map<String, Alarm> alarms = new LinkedHashMap<>();

    @Override
    public synchronized void schedule(Alarm alarm) {
        final Alarm copy = StoreDocuments.copy(alarm);
        copy.setLockedUntil(null);
        alarms.put(alarm.getId(), copy);
    }

    @Override
    public synchronized void cancel(String id) {
        alarms.remove(id);
    }

    @Override
    public synchronized List<Alarm> claimDue(LocalDateTime now, Instant claimedAt, Instant leaseUntil, int limit) {
        final List<Alarm> due = alarms.values().stream()
                .filter(a -> a.getDueAt().isBefore(now) && (a.getLockedUntil() == null || a.getLockedUntil().isBefore(claimedAt)))
                .sorted(Comparator.comparing(Alarm::getDueAt))
                .limit(limit)
                .collect(Collectors.toList());
        due.forEach(a -> {
            a.setLockedUntil(leaseUntil);
            a.setAttempts(a.getAttempts() + 1);
        });
        return due.stream().map(StoreDocuments::copy).collect(Collectors.toList());
    }

    @Override
    public synchronized long countPending() {
        return alarms.size();
    }
}
