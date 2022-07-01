package com.aktimetrix.core.service;

import com.aktimetrix.core.api.*;
import com.aktimetrix.core.impl.StepEventGenerator;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.StepInstanceDTO;
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
@com.aktimetrix.core.stereotypes.PostProcessor(code = "SI_PUBLISHER", processType = Constants.DEFAULT_PROCESS_TYPE, processCode = Constants.DEFAULT_PROCESS_CODE)
public class StepInstancePublisherService implements PostProcessor {

    final private StreamBridge streamBridge;
    @Qualifier("StepEventGenerator")
    final private EventGenerator<StepInstanceDTO, Void> eventGenerator;

    @Override
    public void postProcess(Context context) {
        log.debug("executing step instance publisher service");
        if (context.getStepInstances() == null || context.getStepInstances().isEmpty()) {
            return;
        }
        context.getStepInstances().forEach(step -> {
            Event<StepInstanceDTO, Void> event = eventGenerator.generate(step, context);
            log.debug("step instance event : {}", event);
            final Message<Event<StepInstanceDTO, Void>> message = MessageBuilder.withPayload(event)
                    .setHeader(KafkaHeaders.MESSAGE_KEY, event.getEntityId())
                    .build();
            this.streamBridge.send("step-instance-out-0", message);
        });
    }
}
