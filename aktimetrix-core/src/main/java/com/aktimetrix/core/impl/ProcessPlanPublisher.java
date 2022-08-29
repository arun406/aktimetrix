package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.EventGenerator;
import com.aktimetrix.core.api.Publisher;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.ProcessPlanInstance;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.ProcessPlanDTO;
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
public class ProcessPlanPublisher implements Publisher {

    public static final String PROCESS_PLAN_INSTANCE_TOPIC_NAME = "process-plan-instance-out-0";
    private final StreamBridge streamBridge;
    @Qualifier("ProcessPlanEventGenerator")
    private final EventGenerator<ProcessPlanDTO, Void> eventGenerator;

    /**
     * Publish event based on the context information
     *
     * @param context
     */
    @Override
    public void publish(Context context) {
        log.debug("executing process instance publisher service");
        if (context == null) {
            return;
        }
        ProcessPlanInstance currentProcessPlanInstance = context.getProcessPlanInstance();
        ProcessInstance currentProcessInstance = context.getProcessInstance();
        if (currentProcessInstance == null || currentProcessPlanInstance == null) {
            return;
        }
        createEventAndPublish(currentProcessPlanInstance, currentProcessInstance);

        log.debug("checking and publishing cancelled plan.");
        if (context.containsProperty("cancelled-plan") && context.containsProperty("cancelled-process-instance")) {
            ProcessPlanInstance cancelledProcessPlanInstance = (ProcessPlanInstance) context.getProperty("cancelled-plan");
            ProcessInstance cancelledProcessInstance = (ProcessInstance) context.getProperty("cancelled-process-instance");
            log.debug("publishing the cancelled plan event");
            createEventAndPublish(cancelledProcessPlanInstance, cancelledProcessInstance);
        }

    }

    /**
     * @param currentProcessPlanInstance
     * @param currentProcessInstance
     */
    private void createEventAndPublish(ProcessPlanInstance currentProcessPlanInstance, ProcessInstance currentProcessInstance) {
        final Event<ProcessPlanDTO, Void> event = eventGenerator.generate(currentProcessPlanInstance, currentProcessInstance);
        log.debug("process instance event : {}", event);
        final Message<Event<ProcessPlanDTO, Void>> message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.MESSAGE_KEY, event.getEntityId())
                .build();
        this.streamBridge.send(PROCESS_PLAN_INSTANCE_TOPIC_NAME, message);
    }
}
