package com.aktimetrix.core.impl.publisher;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.EventGenerator;
import com.aktimetrix.core.api.Publisher;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.Measurement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasurementInstancePublisher implements Publisher {

    public static final String MEASUREMENT_INSTANCE_TOPIC_NAME = "measurement-instance-out-0";
    final private StreamBridge streamBridge;
    @Qualifier("MeasurementEventGenerator")
    final private EventGenerator<Measurement, Void> eventGenerator;


    /**
     * Publish event based on the context information
     *
     * @param context
     */
    @Override
    public void publish(Context context) {
        log.debug("executing process instance publisher service");
        if (context.getMeasurementInstances() != null) {
            context.getMeasurementInstances().forEach(m -> {
                Event<Measurement, Void> event = this.eventGenerator.generate(m);
                log.debug("measurement instance event : {}", event);
                final Message<Event<Measurement, Void>> message = MessageBuilder.withPayload(event)
                        .setHeader(KafkaHeaders.MESSAGE_KEY, event.getEntityId())
                        .build();
                this.streamBridge.send(MEASUREMENT_INSTANCE_TOPIC_NAME, message);
            });
        }
    }
}
