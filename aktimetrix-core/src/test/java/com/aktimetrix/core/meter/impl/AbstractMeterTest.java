package com.aktimetrix.core.meter.impl;

import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.stereotypes.Measurement;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractMeterTest {

    private static final LocalDateTime ORDERED_ON = LocalDateTime.of(2022, 5, 22, 23, 46);

    private final ShipMeter meter = new ShipMeter();

    @Test
    void readsMetadataTimeWhateverItsStoredType() {
        assertThat(meter.metadataTime(step(ORDERED_ON), "orderedOn")).isEqualTo(ORDERED_ON);
        assertThat(meter.metadataTime(step("2022-05-22T23:46:00"), "orderedOn")).isEqualTo(ORDERED_ON);
        assertThat(meter.metadataTime(step("2022-05-22 23:46:00"), "orderedOn")).isEqualTo(ORDERED_ON);
        assertThat(meter.metadataTime(step(Date.from(ORDERED_ON.atZone(ZoneId.systemDefault()).toInstant())), "orderedOn"))
                .isEqualTo(ORDERED_ON);
        assertThat(meter.metadataTime(step(ORDERED_ON), "missing")).isNull();
    }

    @Test
    void measuresPlannedValueWithTheAnnotatedCodes() {
        var measurement = meter.measure("AA", step(ORDERED_ON));

        assertThat(measurement.getCode()).isEqualTo("TIME");
        assertThat(measurement.getStepCode()).isEqualTo("SHIP");
        assertThat(measurement.getType()).isEqualTo("P");
        assertThat(measurement.getValue()).isEqualTo("2022-05-23T01:46");
    }

    private static StepInstance step(Object orderedOn) {
        StepInstance step = new StepInstance();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderedOn", orderedOn);
        step.setMetadata(metadata);
        return step;
    }

    @Measurement(code = "TIME", stepCode = "SHIP")
    static class ShipMeter extends AbstractMeter {
        @Override
        protected String getMeasurementUnit(String tenant, StepInstance step) {
            return "TIMESTAMP";
        }

        @Override
        protected String getMeasurementValue(String tenant, StepInstance step) {
            return String.valueOf(metadataTime(step, "orderedOn").plusHours(2));
        }
    }
}
