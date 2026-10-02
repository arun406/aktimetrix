package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.referencedata.model.MetricDefinition;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.store.MeasurementInstanceStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DerivedMetricServiceTest {

    private final MeasurementInstanceStore store = mock(MeasurementInstanceStore.class);
    private final DerivedMetricService service = new DerivedMetricService(store, metrics(), Clock.systemUTC());

    @Test
    void functionsSeeEachValueOfAMeasurementAcrossTheSteps() {
        when(store.findByProcessInstance("AA", "p1")).thenReturn(List.of(
                actual("TEMPERATURE", "21"), actual("TEMPERATURE", "34"), actual("TEMPERATURE", "28"),
                actual("RATING", "4"), planned("TEMPERATURE", "30"), planned("TEMPERATURE", "30")));

        final List<MeasurementInstance> results = service.compute(process(), definition(
                metric("PEAK_TEMPERATURE", "max(TEMPERATURE)", "5%"),
                metric("RATINGS", "count(RATING)", null)));

        assertThat(results).extracting(MeasurementInstance::getCode, MeasurementInstance::getValue,
                MeasurementInstance::getPlannedValue).containsExactly(
                tuple("PEAK_TEMPERATURE", "34", "30"),
                tuple("RATINGS", "1", "0"));
    }

    @Test
    void aLateMeasurementRecomputesOnlyTheMetricsThatUseIt() {
        when(store.findByProcessInstance("AA", "p1")).thenReturn(List.of(
                actual("FUEL", "0.6"), actual("DISTANCE", "12"), actual("RATING", "3"), actual("RATING", "5")));
        final ProcessDefinition definition = definition(metric("FUEL_PER_KM", "FUEL / DISTANCE", null),
                metric("AVERAGE_RATING", "avg(RATING)", null));

        assertThat(service.compute(process(), definition, Set.of("RATING")))
                .singleElement().satisfies(m -> {
                    assertThat(m.getCode()).isEqualTo("AVERAGE_RATING");
                    assertThat(m.getValue()).isEqualTo("4");
                    assertThat(m.getDerivedFrom()).isEqualTo("avg(RATING)");
                });
        assertThat(service.compute(process(), definition)).hasSize(2);
    }

    private static AktimetrixMetrics metrics() {
        @SuppressWarnings("unchecked") final ObjectProvider<MeterRegistry> registry =
                mock(ObjectProvider.class);
        when(registry.getIfAvailable(any())).thenReturn(new SimpleMeterRegistry());
        return new AktimetrixMetrics(registry);
    }

    private static ProcessInstance process() {
        final ProcessInstance process = new ProcessInstance();
        process.setId("p1");
        process.setTenant("AA");
        return process;
    }

    private static ProcessDefinition definition(MetricDefinition... metrics) {
        final ProcessDefinition definition = new ProcessDefinition("AA", "ORDER_DELIVERY");
        definition.setMetrics(List.of(metrics));
        return definition;
    }

    private static MetricDefinition metric(String code, String expression, String tolerance) {
        final MetricDefinition metric = new MetricDefinition();
        metric.setCode(code);
        metric.setExpression(expression);
        metric.setTolerance(tolerance);
        return metric;
    }

    private static MeasurementInstance actual(String code, String value) {
        return new MeasurementInstance("AA", code, value, null, "p1", null, null, Constants.ACTUAL_MEASUREMENT_TYPE,
                null, null);
    }

    private static MeasurementInstance planned(String code, String value) {
        return new MeasurementInstance("AA", code, value, null, "p1", null, null, Constants.PLAN_MEASUREMENT_TYPE,
                null, null);
    }
}
