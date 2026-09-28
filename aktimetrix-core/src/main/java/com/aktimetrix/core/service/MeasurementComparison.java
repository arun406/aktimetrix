package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Conformance;
import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.repository.MeasurementInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Compares an actual measurement with the planned measurement of the same code, for the same step or process: sets
 * the actual's {@code plannedValue}, its {@code deviation} (actual minus planned) and, when the measurement declares a
 * tolerance, its {@link Conformance}. Works for any numeric dimension: distance, fuel, temperature, rating…
 */
@Service
@RequiredArgsConstructor
public class MeasurementComparison {

    private final MeasurementInstanceRepository repository;
    private final AktimetrixMetrics metrics;

    /**
     * @param tolerance the tolerance declared for the measurement, e.g. {@code 2} or {@code 10%}; may be {@code null}
     */
    public void compare(MeasurementInstance actual, String tolerance) {
        repository.findByOwnerAndCodeAndType(actual.getTenant(), actual.getProcessInstanceId(),
                        actual.getStepInstanceId(), actual.getCode(), Constants.PLAN_MEASUREMENT_TYPE).stream()
                .findFirst()
                .ifPresent(planned -> apply(actual, planned.getValue(), tolerance));
        metrics.measurementRecorded(actual);
    }

    /**
     * Sets the comparison of {@code actual} with {@code plannedValue}. Values that are not numbers are only recorded
     * side by side.
     */
    public static void apply(MeasurementInstance actual, String plannedValue, String tolerance) {
        actual.setPlannedValue(plannedValue);
        final BigDecimal planned = number(plannedValue);
        final BigDecimal value = number(actual.getValue());
        if (planned == null || value == null) {
            return;
        }
        final BigDecimal deviation = value.subtract(planned);
        actual.setDeviation(deviation.stripTrailingZeros().toPlainString());
        final BigDecimal allowed = allowed(planned, tolerance);
        if (allowed != null) {
            actual.setConformance(deviation.abs().compareTo(allowed) <= 0
                    ? Conformance.WITHIN_TOLERANCE : Conformance.OUT_OF_TOLERANCE);
        }
    }

    private static BigDecimal allowed(BigDecimal planned, String tolerance) {
        if (tolerance == null || tolerance.isBlank()) {
            return null;
        }
        final String trimmed = tolerance.trim();
        if (trimmed.endsWith("%")) {
            final BigDecimal percent = number(trimmed.substring(0, trimmed.length() - 1));
            return percent == null ? null
                    : planned.abs().multiply(percent).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        }
        return number(trimmed);
    }

    private static BigDecimal number(String text) {
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
