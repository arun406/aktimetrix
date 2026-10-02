package com.aktimetrix.store.jdbc;

import com.aktimetrix.core.model.Alarm;
import com.aktimetrix.core.store.AlarmStore;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Alarms in a relational table. Due alarms are selected in a batch, then each is claimed with a conditional update:
 * of two instances that select the same alarm, only one update matches it.
 */
final class JdbcAlarmStore implements AlarmStore {

    private final JdbcTemplate jdbc;
    private final RowMapper<Alarm> rows = (rs, n) -> new Alarm(rs.getString("id"), rs.getString("tenant"),
            rs.getString("kind"), rs.getString("target_id"), rs.getString("process_instance_id"),
            JdbcTimes.instant(rs.getObject("due_at", LocalDateTime.class)), instant(rs.getObject("locked_until", Long.class)),
            rs.getInt("attempts"));

    JdbcAlarmStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Instant instant(Long millis) {
        return millis == null ? null : Instant.ofEpochMilli(millis);
    }

    @Override
    public void schedule(Alarm alarm) {
        if (update(alarm) > 0) {
            return;
        }
        try {
            jdbc.update("INSERT INTO aktimetrix_alarm (id, tenant, kind, target_id, process_instance_id, due_at, "
                            + "locked_until, attempts) VALUES (?, ?, ?, ?, ?, ?, NULL, ?)", alarm.getId(), alarm.getTenant(),
                    alarm.getKind(), alarm.getTargetId(), alarm.getProcessInstanceId(), JdbcTimes.utc(alarm.getDueAt()), alarm.getAttempts());
        } catch (DuplicateKeyException e) {
            // set at the same moment by another unit of work: replace it
            update(alarm);
        }
    }

    private int update(Alarm alarm) {
        return jdbc.update("UPDATE aktimetrix_alarm SET due_at = ?, locked_until = NULL WHERE id = ?",
                JdbcTimes.utc(alarm.getDueAt()), alarm.getId());
    }

    @Override
    public void cancel(String id) {
        jdbc.update("DELETE FROM aktimetrix_alarm WHERE id = ?", id);
    }

    @Override
    public List<Alarm> claimDue(Instant now, Instant claimedAt, Instant leaseUntil, int limit) {
        final List<String> candidates = jdbc.queryForList("SELECT id FROM aktimetrix_alarm WHERE due_at < ? "
                        + "AND (locked_until IS NULL OR locked_until < ?) ORDER BY due_at LIMIT ?", String.class, JdbcTimes.utc(now),
                claimedAt.toEpochMilli(), limit);
        final List<Alarm> claimed = new ArrayList<>();
        for (String id : candidates) {
            final int won = jdbc.update("UPDATE aktimetrix_alarm SET locked_until = ?, attempts = attempts + 1 "
                            + "WHERE id = ? AND (locked_until IS NULL OR locked_until < ?)", leaseUntil.toEpochMilli(), id,
                    claimedAt.toEpochMilli());
            if (won == 1) {
                jdbc.query("SELECT * FROM aktimetrix_alarm WHERE id = ?", rows, id).stream().findFirst()
                        .ifPresent(claimed::add);
            }
        }
        return claimed;
    }

    @Override
    public long countPending() {
        final Long count = jdbc.queryForObject("SELECT COUNT(*) FROM aktimetrix_alarm", Long.class);
        return count == null ? 0 : count;
    }
}
