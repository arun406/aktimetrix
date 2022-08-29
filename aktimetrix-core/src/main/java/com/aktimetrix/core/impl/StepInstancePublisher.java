package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.EventGenerator;
import com.aktimetrix.core.api.Publisher;
import com.aktimetrix.core.model.StepInstance;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class StepInstancePublisher implements Publisher {

    public static final String STEP_INSTANCE_TOPIC_NAME = "step-instance-out-0";
    final private StreamBridge streamBridge;
    @Qualifier("StepEventGenerator")
    final private EventGenerator<StepInstanceDTO, Void> eventGenerator;

    /**
     * Publish event based on the context information
     *
     * @param context
     */
    @Override
    public void publish(Context context) {
        log.debug("executing step instance publisher service");
        if (context.getStepInstances() == null || context.getStepInstances().isEmpty()) {
            log.debug("nothing to publish");
            return;
        }
        context.getStepInstances().forEach(step -> publish(context, step));
    }

    private void publish(Context context, StepInstance step) {
        Event<StepInstanceDTO, Void> event = eventGenerator.generate(step, context);
        log.debug("step instance event : {}", event);
        final Message<Event<StepInstanceDTO, Void>> message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.MESSAGE_KEY, event.getEntityId())
                .build();
        this.streamBridge.send(STEP_INSTANCE_TOPIC_NAME, message);
    }
}
