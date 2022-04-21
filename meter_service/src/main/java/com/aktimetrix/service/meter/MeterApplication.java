package com.aktimetrix.service.meter;

import com.aktimetrix.service.meter.core.meter.api.MeasurementService;
import com.aktimetrix.service.meter.core.transferobjects.Event;
import com.aktimetrix.service.meter.core.transferobjects.Measurement;
import com.aktimetrix.service.meter.core.transferobjects.StepInstanceDTO;
import com.aktimetrix.service.meter.core.transferobjects.StepMeasurementEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ComponentScan({"com.aktimetrix.service.meter",
        "com.aktimetrix.service.meter.core",
        "com.aktimetrix.service.meter.referencedata"})
@SpringBootApplication
@Slf4j
public class MeterApplication {

    @Autowired
    private MeasurementService meterService;

    public static void main(String[] args) {
        SpringApplication.run(MeterApplication.class, args);
    }

    /**
     * Consumes the StepEvent and prepares and produce the StepMeasurementEvent
     *
     * @return
     */
    @Bean
    public java.util.function.Function<Event<StepInstanceDTO, Void>, List<Message<StepMeasurementEvent>>> measure() {
        return input -> {
            log.debug("event: {}", input);
            final List<Measurement> measurements = getStepMeasurements(input.getTenantKey(), input.getEntity());
            return measurements.stream()
                    .map(this::getStepMeasurementEvent)
                    .map(this::getMessage)
                    .collect(Collectors.toList());
        };
    }

    private Message<StepMeasurementEvent> getMessage(StepMeasurementEvent stepMeasurementEvent) {
        return MessageBuilder.withPayload(stepMeasurementEvent)
//                .setHeader(KafkaHeaders.MESSAGE_KEY, stepMeasurementEvent.getEventId())
                .build();
    }


    /**
     * generates the KeyValue of Measurement Id and Measurement Instance
     *
     * @param stepMeasurementEvent measurement event
     * @return KeyValue
     */
    private KeyValue<String, StepMeasurementEvent> getKeyValue(StepMeasurementEvent stepMeasurementEvent) {
        return new KeyValue<>(stepMeasurementEvent.getEventId(), stepMeasurementEvent);
    }

    /**
     * @param tenantKey tenant
     * @param step      step
     * @return List of Measurement
     */
    private List<Measurement> getStepMeasurements(String tenantKey, StepInstanceDTO step) {
        return meterService.generateMeasurements(tenantKey, step);
    }

    /**
     * Creates Measurement Events
     *
     * @param measurement measurement
     * @return measurement event
     */
    private StepMeasurementEvent getStepMeasurementEvent(Measurement measurement) {
        StepMeasurementEvent event = new StepMeasurementEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("Measurement_Event");
        event.setEventCode("CAPTURED");
        event.setEventName("Step Measurement Created Event");
        event.setEventTime(ZonedDateTime.now());
        event.setEventUTCTime(LocalDateTime.now(ZoneOffset.UTC));
        event.setEntityId(measurement.getId());
        event.setEntityType("com.aktimetrix.measurement");
        event.setSource("Meter");
        event.setTenantKey(measurement.getTenant());
        event.setEventDetails(measurement);
        return event;
    }
}
