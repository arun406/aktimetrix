package com.aktimetrix.store.jdbc;

import com.aktimetrix.core.store.ProcessedEventStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

/**
 * Processed events in a relational table, keyed by tenant and event id: a second insert of the same event fails with
 * a {@link org.springframework.dao.DuplicateKeyException}.
 */
final class JdbcProcessedEventStore implements ProcessedEventStore {

    private final JdbcTemplate jdbc;

    JdbcProcessedEventStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isProcessed(String tenant, String eventId) {
        final Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM aktimetrix_processed_event WHERE tenant = ? "
                + "AND event_id = ?", Integer.class, tenant, eventId);
        return count != null && count > 0;
    }

    @Override
    public void markProcessed(String tenant, String eventId, Instant processedAt) {
        jdbc.update("INSERT INTO aktimetrix_processed_event (tenant, event_id, processed_at) VALUES (?, ?, ?)", tenant,
                eventId, processedAt.toEpochMilli());
    }

    @Override
    public long deleteProcessedBefore(Instant before) {
        return jdbc.update("DELETE FROM aktimetrix_processed_event WHERE processed_at < ?", before.toEpochMilli());
    }
}
