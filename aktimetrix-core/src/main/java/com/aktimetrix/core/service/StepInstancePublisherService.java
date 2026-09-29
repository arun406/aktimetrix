package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.api.PublishedEvents;
import com.aktimetrix.core.impl.StepEventGenerator;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.outbox.Outbox;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.EventContext;
import com.aktimetrix.core.transferobjects.StepInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@com.aktimetrix.core.stereotypes.PostProcessor(code = "SI_PUBLISHER", processType = Constants.ALL_PROCESS_TYPES, priority = Constants.BUILT_IN_PRIORITY)
public class StepInstancePublisherService implements PostProcessor {

    final private Outbox outbox;
    final private PublishedEventContexts contexts;

    @Override
    public void postProcess(Context context) {
        log.debug("executing process instance publisher service");
        if (context.getStepInstances() == null || context.getStepInstances().isEmpty()) {
            return;
        }
        context.getStepInstances().forEach(step -> publish(step, PublishedEvents.Step.CREATED));
    }

    /**
     * Publishes a change of the step instance to {@code step-instance-out-0}.
     *
     * @param eventCode what happened: CREATED, STARTED, COMPLETED or OVERDUE
     */
    /**
     * Publishes a {@code Step_Event}, keyed by the id of the step's process instance, so that a process's step
     * events stay in order.
     *
     * @param eventCode see {@link com.aktimetrix.core.api.PublishedEvents.Step}
     */
    public void publish(StepInstance step, String eventCode) {
        final Event<StepInstanceDTO, EventContext> event = new StepEventGenerator(step, eventCode, contexts.of(step),
                contexts.definitionOf(step.getTenant(), step.getProcessInstanceId())).generate();
        log.debug("step instance event : {}", event);
        outbox.enqueue("step-instance-out-0", step.getProcessInstanceId(), event);
    }
}
