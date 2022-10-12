package com.aktimetrix.core.impl.publisher;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.EventGenerator;
import com.aktimetrix.core.api.Publisher;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.ProcessInstanceDTO;
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
public class ProcessInstancePublisher implements Publisher {

    public static final String PROCESS_INSTANCE_TOPIC_NAME = "process-instance-out-0";

    private final StreamBridge streamBridge;
    @Qualifier("ProcessEventGenerator")
    private final EventGenerator<ProcessInstanceDTO, Void> eventGenerator;


    /**
     * Publish event based on the context information
     *
     * @param context
     */
    @Override
    public void publish(Context context) {
        log.debug("executing process instance publisher service");
        ProcessInstance processInstance = context.getProcessInstance();
        final Event<ProcessInstanceDTO, Void> event = this.eventGenerator.generate(processInstance);
        log.debug("process instance event : {}", event);
        final Message<Event<ProcessInstanceDTO, Void>> message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.MESSAGE_KEY, event.getEntityId())
                .build();
        this.streamBridge.send(PROCESS_INSTANCE_TOPIC_NAME, message);
    }
}
