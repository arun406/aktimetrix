package com.aktimetrix.store.jdbc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Times in {@code TIMESTAMP} columns are UTC date-times: every JDBC driver binds a {@link LocalDateTime}, and the
 * column compares them as the instants they stand for.
 */
final class JdbcTimes {

    private JdbcTimes() {
    }

    static LocalDateTime utc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    static Instant instant(LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }
}
