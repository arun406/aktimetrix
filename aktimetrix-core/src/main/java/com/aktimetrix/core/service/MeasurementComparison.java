package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Conformance;
import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.store.MeasurementInstanceStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Compares an actual measurement with the planned measurement of the same code, for the same step or process: sets
 * the actual's {@code plannedValue}, its {@code deviation} (actual minus planned) and, when the measurement declares a
 * tolerance or a bad direction ({@code worseWhen}), its {@link Conformance}. Works for any numeric dimension:
 * distance, fuel, temperature, rating…
 */
@Service
@RequiredArgsConstructor
public class MeasurementComparison {

    private final MeasurementInstanceStore store;
    private final AktimetrixMetrics metrics;

    /**
     * @param tolerance the tolerance declared for the measurement, e.g. {@code 2} or {@code 10%}; may be {@code null}
     * @param worseWhen {@code HIGHER} or {@code LOWER}, the direction of a bad deviation; may be {@code null} for both
     */
    public void compare(MeasurementInstance actual, String tolerance, String worseWhen) {
        store.find(actual.getTenant(), actual.getProcessInstanceId(),
                        actual.getStepInstanceId(), actual.getCode(), Constants.PLAN_MEASUREMENT_TYPE).stream()
                .findFirst()
                .ifPresent(planned -> apply(actual, planned.getValue(), tolerance, worseWhen));
        if (!actual.isInterim()) {
            metrics.measurementRecorded(actual);
        }
    }

    /**
     * Compares {@code actual} with {@code plannedValue}, counting deviations both ways.
     */
    public static void apply(MeasurementInstance actual, String plannedValue, String tolerance) {
        apply(actual, plannedValue, tolerance, null);
    }

    /**
     * Sets the comparison of {@code actual} with {@code plannedValue}. Values that are not numbers are only recorded
     * side by side.
     */
    public static void apply(MeasurementInstance actual, String plannedValue, String tolerance, String worseWhen) {
        actual.setPlannedValue(plannedValue);
        final BigDecimal planned = number(plannedValue);
        final BigDecimal value = number(actual.getValue());
        if (planned == null || value == null) {
            return;
        }
        final BigDecimal deviation = value.subtract(planned);
        actual.setDeviation(deviation.stripTrailingZeros().toPlainString());
        final String worse = worseWhen == null ? null : worseWhen.trim().toUpperCase();
        BigDecimal allowed = allowed(planned, tolerance);
        if (allowed == null && ("HIGHER".equals(worse) || "LOWER".equals(worse))) {
            allowed = BigDecimal.ZERO;   // at most, or at least, the plan
        }
        if (allowed == null) {
            return;
        }
        final boolean harmless = ("HIGHER".equals(worse) && deviation.signum() <= 0)
                || ("LOWER".equals(worse) && deviation.signum() >= 0);
        actual.setConformance(harmless || deviation.abs().compareTo(allowed) <= 0
                ? Conformance.WITHIN_TOLERANCE : Conformance.OUT_OF_TOLERANCE);
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
