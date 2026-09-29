package com.aktimetrix.core.service;

import java.util.UUID;

import com.aktimetrix.core.api.MeasurementType;
import com.aktimetrix.core.meter.api.ProcessMeter;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.MeasurementDefinition;
import com.aktimetrix.core.transferobjects.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActualMeasurementServiceTest {

    @Mock
    private RegistryService registryService;
    @Mock
    private MeasurementComparison comparison;

    private ActualMeasurementService service() {
        return new ActualMeasurementService(registryService, Clock.systemUTC(), comparison);
    }

    @Test
    void readsAStepActualFromTheCompletingEvent() {
        StepInstance deliver = new StepInstance();
        deliver.setId(UUID.randomUUID().toString());
        deliver.setTenant("AA");
        deliver.setStepCode("DELIVER");
        Event<Object, Object> delivered = Event.of("AA", "ORDER_DELIVERED_EVENT", "com.ecom.order", "1234", ZonedDateTime.now());
        delivered.setEntity(Map.of("route", Map.of("distanceKm", 12.4)));

        List<MeasurementInstance> actuals = service().forStep(deliver,
                List.of(measurement("DISTANCE", MeasurementType.A, "route.distanceKm", "KM"),
                        measurement("DISTANCE", MeasurementType.P, null, null),
                        measurement("TIME", MeasurementType.A, null, null)),
                delivered);

        assertThat(actuals).singleElement().satisfies(distance -> {
            assertThat(distance.getCode()).isEqualTo("DISTANCE");
            assertThat(distance.getValue()).isEqualTo("12.4");
            assertThat(distance.getUnit()).isEqualTo("KM");
            assertThat(distance.getType()).isEqualTo("A");
            assertThat(distance.getStepInstanceId()).isEqualTo(deliver.getId());
        });
    }

    @Test
    void asksTheProcessMeterWhenTheMeasurementDoesNotSayWhereToRead() {
        ProcessInstance order = new ProcessInstance();
        order.setId(UUID.randomUUID().toString());
        order.setTenant("AA");
        order.setProcessCode("ORDER_DELIVERY");
        Event<Object, Object> rated = Event.of("AA", "ORDER_RATED_EVENT", "com.ecom.order", "1234", ZonedDateTime.now());
        MeasurementInstance rating = new MeasurementInstance();
        rating.setCode("RATING");
        rating.setValue("4");
        ProcessMeter ratingMeter = new ProcessMeter() {
            @Override
            public MeasurementInstance measure(String tenant, ProcessInstance process) {
                return null;
            }

            @Override
            public MeasurementInstance measureActual(String tenant, ProcessInstance process, Event<?, ?> event) {
                return rating;
            }
        };
        when(registryService.getProcessMeter("AA", "ORDER_DELIVERY", "RATING")).thenReturn(ratingMeter);

        assertThat(service().forProcess(order, List.of(measurement("RATING", MeasurementType.A, null, null)), rated))
                .containsExactly(rating);
    }

    @Test
    void recordsNothingWhenTheValueIsMissing() {
        StepInstance step = new StepInstance();
        step.setTenant("AA");
        assertThat(service().forStep(step, List.of(measurement("WEIGHT", MeasurementType.A, "weightKg", "KG")),
                Event.of("AA", "X", "t", "1", ZonedDateTime.now()))).isEmpty();
    }

    private static MeasurementDefinition measurement(String code, MeasurementType type, String valueFrom, String unit) {
        MeasurementDefinition measurement = new MeasurementDefinition();
        measurement.setMeasurementCode(code);
        measurement.setType(type);
        measurement.setValueFrom(valueFrom);
        measurement.setUnit(unit);
        return measurement;
    }
}
