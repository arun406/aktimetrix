package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Conformance;
import com.aktimetrix.core.model.MeasurementInstance;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeasurementComparisonTest {

    @Test
    void distanceTravelledBeyondThePlannedToleranceIsOutOfTolerance() {
        MeasurementInstance distance = actual("DISTANCE", "12");

        MeasurementComparison.apply(distance, "5", "20%");

        assertThat(distance.getPlannedValue()).isEqualTo("5");
        assertThat(distance.getDeviation()).isEqualTo("7");
        assertThat(distance.getConformance()).isEqualTo(Conformance.OUT_OF_TOLERANCE);
    }

    @Test
    void aRatingWithinAnAbsoluteToleranceIsWithinTolerance() {
        MeasurementInstance rating = actual("RATING", "4");

        MeasurementComparison.apply(rating, "5", "1");

        assertThat(rating.getDeviation()).isEqualTo("-1");
        assertThat(rating.getConformance()).isEqualTo(Conformance.WITHIN_TOLERANCE);
    }

    @Test
    void withoutAToleranceTheDeviationIsRecordedButNotJudged() {
        MeasurementInstance temperature = actual("TEMPERATURE", "40.5");

        MeasurementComparison.apply(temperature, "30", null);

        assertThat(temperature.getDeviation()).isEqualTo("10.5");
        assertThat(temperature.getConformance()).isNull();
    }

    @Test
    void valuesThatAreNotNumbersAreOnlyRecordedSideBySide() {
        MeasurementInstance grade = actual("GRADE", "B");

        MeasurementComparison.apply(grade, "A", "1");

        assertThat(grade.getPlannedValue()).isEqualTo("A");
        assertThat(grade.getDeviation()).isNull();
        assertThat(grade.getConformance()).isNull();
    }

    private static MeasurementInstance actual(String code, String value) {
        MeasurementInstance measurement = new MeasurementInstance();
        measurement.setCode(code);
        measurement.setValue(value);
        measurement.setType("A");
        return measurement;
    }
}
