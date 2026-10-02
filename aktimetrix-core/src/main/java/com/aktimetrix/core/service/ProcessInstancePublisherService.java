package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.impl.ProcessEventGenerator;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.notification.Notifications;
import com.aktimetrix.core.outbox.Outbox;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.EventContext;
import com.aktimetrix.core.transferobjects.ProcessInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@com.aktimetrix.core.stereotypes.PostProcessor(code = "PI_PUBLISHER", processType = Constants.ALL_PROCESS_TYPES, priority = Constants.BUILT_IN_PRIORITY)
public class ProcessInstancePublisherService implements PostProcessor {

    final private Outbox outbox;
    final private PublishedEventContexts contexts;
    private final Notifications notifications;

    @Override
    public void postProcess(Context context) {
        log.debug("executing process instance publisher service");
        publish(context.getProcessInstance(), "CREATED");
    }

    /**
     * Queues a process event, e.g. {@code COMPLETED}, for {@code process-instance-out-0}.
     */
    /**
     * Publishes a {@code Process_Event}, keyed by the process instance id.
     *
     * @param eventCode see {@link com.aktimetrix.core.api.PublishedEvents.Process}
     */
    public void publish(ProcessInstance processInstance, String eventCode) {
        final Event<ProcessInstanceDTO, EventContext> event =
                new ProcessEventGenerator(processInstance, eventCode, contexts.of(processInstance)).generate();
        log.debug("process instance event : {}", event);
        outbox.enqueue("process-instance-out-0", processInstance.getId(), event);
        notifications.onProcess(processInstance, eventCode, event);
    }
}
