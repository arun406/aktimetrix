package com.aktimetrix.core.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

/**
 * Aktimetrix keeps every time as a UTC {@link Instant}. These helpers read the other forms a time may arrive in: a
 * date-time with an offset or zone, a {@link Date}, or a date-time without one, which is taken as UTC, such as
 * {@code 2024-01-10T09:00} or {@code 2024-01-10 09:00:00} kept in metadata or stored by earlier versions.
 */
public final class Times {

    private Times() {
    }

    /**
     * The instant a value stands for.
     *
     * @return {@code null} when the value is {@code null}
     * @throws DateTimeParseException when a text is not a date-time
     * @throws IllegalArgumentException when the value is of another type
     */
    public static Instant toInstant(Object value) {
        if (value == null || value instanceof Instant) {
            return (Instant) value;
        }
        if (value instanceof Date) {
            return ((Date) value).toInstant();
        }
        if (value instanceof ZonedDateTime) {
            return ((ZonedDateTime) value).toInstant();
        }
        if (value instanceof OffsetDateTime) {
            return ((OffsetDateTime) value).toInstant();
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toInstant(ZoneOffset.UTC);
        }
        if (value instanceof CharSequence) {
            return parse(value.toString());
        }
        if (value instanceof TemporalAccessor) {
            return Instant.from((TemporalAccessor) value);
        }
        throw new IllegalArgumentException("Not a date-time: " + value.getClass().getName());
    }

    /**
     * Reads an ISO-8601 instant, a date-time with an offset, or a date-time without one, taken as UTC; a space may
     * separate the date and the time.
     *
     * @throws DateTimeParseException when the text is not a date-time
     */
    public static Instant parse(String text) {
        final String iso = text.trim().replace(' ', 'T');
        try {
            return OffsetDateTime.parse(iso).toInstant();
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(iso).toInstant(ZoneOffset.UTC);
        }
    }
}
