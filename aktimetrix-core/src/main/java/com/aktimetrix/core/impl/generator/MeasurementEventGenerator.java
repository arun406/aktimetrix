package com.aktimetrix.core.impl.generator;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.EventGenerator;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.Measurement;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Component("MeasurementEventGenerator")
public class MeasurementEventGenerator implements EventGenerator<Measurement, Void> {


    /**
     * Generate the Events
     *
     * @return Event
     */
    @Override
    public Event<Measurement, Void> generate(Object... object) {
        return getMeasurementEvent((MeasurementInstance) object[0]);
    }

    private Event<Measurement, Void> getMeasurementEvent(MeasurementInstance instance) {
        Event<Measurement, Void> event = new Event<>();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(Constants.MEASUREMENT_EVENT);
        event.setEventCode(Constants.MEASUREMENT_CREATED);
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
                .id(instance.getId())
                .tenant(instance.getTenant())
                .stepCode(instance.getStepCode())
                .stepInstanceId(instance.getStepInstanceId())
                .measuredAt(instance.getMeasuredAt())
                .metadata(instance.getMetadata())
                .code(instance.getCode())
                .unit(instance.getUnit())
                .createdOn(instance.getCreatedOn())
                .type(instance.getType())
                .value(instance.getValue())
                .processInstanceId(instance.getProcessInstanceId())
                .createdOn(instance.getCreatedOn())
                .build();
    }
}
