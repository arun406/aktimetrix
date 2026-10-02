package com.aktimetrix.core.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimesTest {

    private static final Instant NINE = Instant.parse("2024-01-10T09:00:00Z");

    @Test
    void everyFormOfATimeIsReadAsTheInstantItStandsFor() {
        assertThat(Times.toInstant(NINE)).isEqualTo(NINE);
        assertThat(Times.toInstant(Date.from(NINE))).isEqualTo(NINE);
        assertThat(Times.toInstant(ZonedDateTime.of(2024, 1, 10, 10, 0, 0, 0, ZoneOffset.ofHours(1)))).isEqualTo(NINE);
        assertThat(Times.toInstant(LocalDateTime.of(2024, 1, 10, 9, 0))).as("without an offset: UTC").isEqualTo(NINE);
        assertThat(Times.toInstant("2024-01-10T09:00:00Z")).isEqualTo(NINE);
        assertThat(Times.toInstant("2024-01-10T14:30:00+05:30")).isEqualTo(NINE);
        assertThat(Times.toInstant("2024-01-10 09:00:00")).isEqualTo(NINE);
        assertThat(Times.toInstant("2024-01-10T09:00")).isEqualTo(NINE);
        assertThat(Times.toInstant(null)).isNull();
    }

    @Test
    void textThatIsNotATimeIsRefused() {
        assertThatThrownBy(() -> Times.parse("tomorrow")).isInstanceOf(DateTimeParseException.class);
    }
}
