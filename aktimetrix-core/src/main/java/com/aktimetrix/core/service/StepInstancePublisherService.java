package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.api.ProcessType;
import com.aktimetrix.core.impl.StepEventGenerator;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.StepInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@com.aktimetrix.core.stereotypes.PostProcessor(code = "SI_PUBLISHER", processType = ProcessType.A2ATRANSPORT)
public class StepInstancePublisherService implements PostProcessor {

    final private StreamBridge streamBridge;

    @Override
    public void postProcess(Context context) {
        log.debug("executing process instance publisher service");
        if (context.getStepInstances() == null || context.getStepInstances().isEmpty()) {
            return;
        }
        context.getStepInstances().forEach(step -> {
            StepEventGenerator eventGenerator = new StepEventGenerator(step);
            Event<StepInstanceDTO, Void> event = eventGenerator.generate();
            log.debug("step instance event : {}", event);
            final Message<Event<StepInstanceDTO, Void>> message = MessageBuilder.withPayload(event)
                    .setHeader(KafkaHeaders.MESSAGE_KEY, event.getEntityId())
                    .build();
            this.streamBridge.send("step-instance-out-0", message);
        });
    }
}
