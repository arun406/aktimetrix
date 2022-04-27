package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.EventGenerator;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.Measurement;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public class MeasurementEventGenerator implements EventGenerator {

    private final MeasurementInstance instance;

    public MeasurementEventGenerator(MeasurementInstance instance) {
        this.instance = instance;
    }

    /**
     * Generate the Events
     *
     * @return Event
     */
    @Override
    public Event<Measurement, Void> generate() {
        return getMeasurementEvent(this.instance);
    }

    private Event<Measurement, Void> getMeasurementEvent(MeasurementInstance instance) {
        Event<Measurement, Void> event = new Event<>();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("Measurement_Event");
        event.setEventCode("CREATED");
        event.setEventName("Measurement Instance Created Event");
        event.setEventTime(ZonedDateTime.now());
        event.setEventUTCTime(LocalDateTime.now(ZoneOffset.UTC));
        event.setEntityId(String.valueOf(instance.getId()));
        event.setEntityType("com.aktimetrix.measurement.instance");
        event.setSource("Meter");
        event.setTenantKey(instance.getTenant());
        event.setEntity(getMeasurement(instance));
        return event;
    }

    private Measurement getMeasurement(MeasurementInstance instance) {
        return Measurement.builder()
                .id(instance.getId().toString())
                .tenant(instance.getTenant())
                .stepCode(instance.getStepCode())
                .stepInstanceId(instance.getStepInstanceId().toString())
                .measuredAt(instance.getMeasuredAt())
                .code(instance.getCode())
                .unit(instance.getUnit())
                .createdOn(instance.getCreatedOn())
                .type(instance.getType())
                .value(instance.getValue())
                .processInstanceId(instance.getProcessInstanceId().toString())
                .createdOn(instance.getCreatedOn())
                .build();
    }
}
