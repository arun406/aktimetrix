package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.referencedata.model.MetricDefinition;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.store.MeasurementInstanceStore;
import com.aktimetrix.core.util.Expression;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes a completed process's {@link MetricDefinition metrics} from its measurements, e.g. fuel per kilometre: once
 * from the actual values and once from the planned ones, which are then compared. A measurement code in an expression
 * stands for the sum of that measurement's final values across the process instance and its steps; a function such as
 * {@code max(TEMPERATURE)} sees each value. A metric is computed again when a measurement it uses is recorded after
 * the process completed, such as a rating the next day; the latest value supersedes the earlier one.
 */
@Service
@RequiredArgsConstructor
public class DerivedMetricService {
    private static final Logger logger = LoggerFactory.getLogger(DerivedMetricService.class);

    private final MeasurementInstanceStore store;
    private final AktimetrixMetrics metrics;
    private final Clock clock;

    /**
     * @return the metrics, as process-level actual measurements with their plan, deviation and conformance
     */
    public List<MeasurementInstance> compute(ProcessInstance process, ProcessDefinition definition) {
        return compute(process, definition, null);
    }

    /**
     * The metrics that use any of the measurement codes, such as one recorded after the process completed.
     *
     * @param codes measurement codes; {@code null} for every metric
     */
    public List<MeasurementInstance> compute(ProcessInstance process, ProcessDefinition definition, Set<String> codes) {
        final List<MeasurementInstance> results = new ArrayList<>();
        if (definition == null || definition.getMetrics() == null || definition.getMetrics().isEmpty()) {
            return results;
        }
        final Map<String, List<BigDecimal>> actual = new HashMap<>();
        final Map<String, List<BigDecimal>> planned = new HashMap<>();
        for (MeasurementInstance measurement : store.findByProcessInstance(process.getTenant(), process.getId())) {
            if (measurement.isInterim() || measurement.getDerivedFrom() != null) {
                continue;
            }
            final BigDecimal value = number(measurement.getValue());
            if (value == null) {
                continue;
            }
            final Map<String, List<BigDecimal>> values =
                    Constants.PLAN_MEASUREMENT_TYPE.equals(measurement.getType()) ? planned : actual;
            values.computeIfAbsent(measurement.getCode(), code -> new ArrayList<>()).add(value);
        }
        for (MetricDefinition metric : definition.getMetrics()) {
            if (codes != null && Collections.disjoint(codes, Expression.names(metric.getExpression()))) {
                continue;
            }
            final BigDecimal value;
            final BigDecimal plan;
            try {
                value = Expression.evaluateAll(metric.getExpression(), actual);
                plan = Expression.evaluateAll(metric.getExpression(), planned);
            } catch (IllegalArgumentException e) {
                logger.warn("Metric {} of the {} process cannot be computed: {}", metric.getCode(),
                        definition.getProcessCode(), e.getMessage());
                continue;
            }
            if (value == null) {
                logger.debug("Metric {} of process instance {}: a measurement is missing", metric.getCode(), process.getId());
                continue;
            }
            final MeasurementInstance result = new MeasurementInstance(process.getTenant(), metric.getCode(),
                    plain(value), metric.getUnit(), process.getId(), null, null, Constants.ACTUAL_MEASUREMENT_TYPE,
                    null, ZonedDateTime.now(clock));
            result.setDerivedFrom(metric.getExpression());
            if (plan != null) {
                MeasurementComparison.apply(result, plain(plan), metric.getTolerance(), metric.getWorseWhen());
            }
            metrics.measurementRecorded(result);
            results.add(result);
        }
        return results;
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal number(String text) {
        try {
            return text == null ? null : new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
