package com.aktimetrix.core.store;

import com.aktimetrix.core.model.StepInstance;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class StoreDocumentsTest {

    @Test
    void timesAreStoredAsUtcInstants() {
        final StepInstance step = new StepInstance();
        step.setPlannedAt(Instant.parse("2024-01-10T12:00:00Z"));

        assertThat(StoreDocuments.toJson(step)).contains("\"plannedAt\":\"2024-01-10T12:00:00Z\"");
        assertThat(StoreDocuments.copy(step).getPlannedAt()).isEqualTo(step.getPlannedAt());
    }

    @Test
    void aTimeStoredByAnEarlierVersionWithoutAnOffsetIsReadAsUtc() {
        final StepInstance step = StoreDocuments.fromJson(
                "{\"stepCode\":\"SORT\",\"plannedAt\":\"2024-01-10T12:00:00\",\"actualAt\":\"2024-01-10T12:10\"}",
                StepInstance.class);

        assertThat(step.getPlannedAt()).isEqualTo(Instant.parse("2024-01-10T12:00:00Z"));
        assertThat(step.getActualAt()).isEqualTo(Instant.parse("2024-01-10T12:10:00Z"));
    }
}
