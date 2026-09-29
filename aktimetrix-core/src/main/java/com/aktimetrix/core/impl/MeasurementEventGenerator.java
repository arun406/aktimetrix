package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.EventGenerator;
import com.aktimetrix.core.api.PublishedEvents;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.EventContext;
import com.aktimetrix.core.transferobjects.Measurement;

/**
 * Generates a {@code Measurement_Event}: the measurement, and its {@link EventContext}. Its code says what kind of
 * measurement it is: {@code PLANNED}, {@code RECORDED}, {@code READING} or {@code METRIC}.
 */
public class MeasurementEventGenerator implements EventGenerator {

    private final MeasurementInstance instance;
    private final EventContext context;

    public MeasurementEventGenerator(MeasurementInstance instance, EventContext context) {
        this.instance = instance;
        this.context = context;
    }

    /**
     * The event code for the measurement.
     */
    public static String codeOf(MeasurementInstance measurement) {
        if (measurement.getDerivedFrom() != null) {
            return PublishedEvents.Measurement.METRIC;
        }
        if (Constants.PLAN_MEASUREMENT_TYPE.equals(measurement.getType())) {
            return PublishedEvents.Measurement.PLANNED;
        }
        return measurement.isInterim() ? PublishedEvents.Measurement.READING : PublishedEvents.Measurement.RECORDED;
    }

    @Override
    public Event<Measurement, EventContext> generate() {
        return EventEnvelopes.envelope(instance.getTenant(), PublishedEvents.Measurement.TYPE,
                PublishedEvents.Measurement.ENTITY_TYPE, codeOf(instance), "Measurement", instance.getId(),
                dto(instance), context);
    }

    private static Measurement dto(MeasurementInstance instance) {
        return Measurement.builder()
                .id(instance.getId())
                .tenant(instance.getTenant())
                .processInstanceId(instance.getProcessInstanceId())
                .stepInstanceId(instance.getStepInstanceId())
                .stepCode(instance.getStepCode())
                .code(instance.getCode())
                .type(instance.getType())
                .value(instance.getValue())
                .unit(instance.getUnit())
                .measuredAt(instance.getMeasuredAt())
                .plannedValue(instance.getPlannedValue())
                .deviation(instance.getDeviation())
                .conformance(instance.getConformance())
                .interim(instance.isInterim())
                .derivedFrom(instance.getDerivedFrom())
                .createdOn(instance.getCreatedOn())
                .build();
    }
}
