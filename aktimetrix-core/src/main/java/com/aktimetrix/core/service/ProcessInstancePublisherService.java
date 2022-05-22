package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.api.ProcessType;
import com.aktimetrix.core.impl.ProcessEventGenerator;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.ProcessInstanceDTO;
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
@com.aktimetrix.core.stereotypes.PostProcessor(code = "PI_PUBLISHER", processType = "A2ATRANSPORT")
public class ProcessInstancePublisherService implements PostProcessor {

    final private StreamBridge streamBridge;

    @Override
    public void postProcess(Context context) {
        log.debug("executing process instance publisher service");
        ProcessEventGenerator eventGenerator = new ProcessEventGenerator(context.getProcessInstance());
        final Event<ProcessInstanceDTO, Void> event = eventGenerator.generate();
        log.debug("process instance event : {}", event);
        final Message<Event<ProcessInstanceDTO, Void>> message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.MESSAGE_KEY, event.getEntityId())
                .build();
        this.streamBridge.send("process-instance-out-0", message);
    }
}
