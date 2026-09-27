package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.impl.StepEventGenerator;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.outbox.Outbox;
import com.aktimetrix.core.transferobjects.Event;
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

    @Override
    public void postProcess(Context context) {
        log.debug("executing process instance publisher service");
        if (context.getStepInstances() == null || context.getStepInstances().isEmpty()) {
            return;
        }
        context.getStepInstances().forEach(step -> publish(step, "CREATED"));
    }

    /**
     * Publishes a change of the step instance to {@code step-instance-out-0}.
     *
     * @param eventCode what happened: CREATED, STARTED, COMPLETED or OVERDUE
     */
    public void publish(StepInstance step, String eventCode) {
        Event<StepInstanceDTO, Void> event = new StepEventGenerator(step, eventCode).generate();
        log.debug("step instance event : {}", event);
        outbox.enqueue("step-instance-out-0", event.getEntityId(), event);
    }
}
