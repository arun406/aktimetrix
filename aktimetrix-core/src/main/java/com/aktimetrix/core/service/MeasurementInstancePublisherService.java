package com.aktimetrix.core.service;

import com.aktimetrix.core.api.*;
import com.aktimetrix.core.impl.MeasurementEventGenerator;
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

@Service
@RequiredArgsConstructor
@Slf4j
@com.aktimetrix.core.stereotypes.PostProcessor(code = "MI_PUBLISHER", processCode = Constants.DEFAULT_PROCESS_CODE, processType = Constants.DEFAULT_PROCESS_TYPE)
public class MeasurementInstancePublisherService implements PostProcessor {

    final private StreamBridge streamBridge;
    @Qualifier("MeasurementEventGenerator")
    final private EventGenerator<Measurement, Void> eventGenerator;

    @Override
    public void postProcess(Context context) {
        log.debug("executing process instance publisher service");
        if (context.getMeasurementInstances() != null) {
            context.getMeasurementInstances().forEach(m -> {
                Event<Measurement, Void> event = eventGenerator.generate(m);
                log.debug("measurement instance event : {}", event);
                final Message<Event<Measurement, Void>> message = MessageBuilder.withPayload(event)
                        .setHeader(KafkaHeaders.MESSAGE_KEY, event.getEntityId())
                        .build();
                this.streamBridge.send("measurement-instance-out-0", message);
            });
        }
    }
}
