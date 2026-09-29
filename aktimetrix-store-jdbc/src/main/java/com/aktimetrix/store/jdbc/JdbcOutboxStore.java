package com.aktimetrix.store.jdbc;

import com.aktimetrix.core.outbox.OutboxMessage;
import com.aktimetrix.core.store.OutboxStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The outbox in a relational table. A message is claimed with a conditional update: of two relays that pick the same
 * message, only one update matches it, and the other moves on to the next.
 */
final class JdbcOutboxStore implements OutboxStore {

    private static final int CLAIM_ATTEMPTS = 5;
    private final JdbcTemplate jdbc;
    private final RowMapper<OutboxMessage> rows = (rs, n) -> {
        final OutboxMessage message = new OutboxMessage(rs.getString("destination"), rs.getString("message_key"),
                rs.getString("payload"), Instant.ofEpochMilli(rs.getLong("created_at")));
        message.setId(rs.getString("id"));
        message.setSentAt(instant(rs.getObject("sent_at", Long.class)));
        message.setLockedUntil(instant(rs.getObject("locked_until", Long.class)));
        message.setAttempts(rs.getInt("attempts"));
        return message;
    };

    JdbcOutboxStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Instant instant(Long millis) {
        return millis == null ? null : Instant.ofEpochMilli(millis);
    }

    private static Long millis(Instant instant) {
        return instant == null ? null : instant.toEpochMilli();
    }

    @Override
    public void add(OutboxMessage message) {
        if (message.getId() == null) {
            message.setId(JdbcInstanceStores.newId());
        }
        jdbc.update("INSERT INTO aktimetrix_outbox (id, destination, message_key, payload, created_at, sent_at, locked_until, "
                        + "attempts) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", message.getId(), message.getDestination(),
                message.getMessageKey(), message.getPayload(), millis(message.getCreatedAt()), millis(message.getSentAt()),
                millis(message.getLockedUntil()), message.getAttempts());
    }

    @Override
    public Optional<OutboxMessage> claimNext(Instant now, Instant leaseUntil) {
        for (int attempt = 0; attempt < CLAIM_ATTEMPTS; attempt++) {
            final List<String> candidates = jdbc.queryForList("SELECT id FROM aktimetrix_outbox WHERE sent_at IS NULL "
                            + "AND (locked_until IS NULL OR locked_until < ?) ORDER BY created_at, row_order LIMIT 1",
                    String.class, now.toEpochMilli());
            if (candidates.isEmpty()) {
                return Optional.empty();
            }
            final String id = candidates.get(0);
            final int claimed = jdbc.update("UPDATE aktimetrix_outbox SET locked_until = ?, attempts = attempts + 1 "
                            + "WHERE id = ? AND sent_at IS NULL AND (locked_until IS NULL OR locked_until < ?)",
                    leaseUntil.toEpochMilli(), id, now.toEpochMilli());
            if (claimed == 1) {
                return jdbc.query("SELECT * FROM aktimetrix_outbox WHERE id = ?", rows, id).stream().findFirst();
            }
            // another relay claimed it first: try the next one
        }
        return Optional.empty();
    }

    @Override
    public void markSent(String id, Instant sentAt) {
        jdbc.update("UPDATE aktimetrix_outbox SET sent_at = ?, locked_until = NULL WHERE id = ?", sentAt.toEpochMilli(), id);
    }

    @Override
    public long countPending() {
        final Long count = jdbc.queryForObject("SELECT COUNT(*) FROM aktimetrix_outbox WHERE sent_at IS NULL", Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public long deleteSentBefore(Instant sentBefore) {
        return jdbc.update("DELETE FROM aktimetrix_outbox WHERE sent_at < ?", sentBefore.toEpochMilli());
    }
}
