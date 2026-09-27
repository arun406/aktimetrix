package com.aktimetrix.core.event.handler;

import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.service.ProcessInstanceService;
import com.aktimetrix.core.service.StepProgressService;
import com.aktimetrix.core.transferobjects.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Base class for handlers of milestone events: business events that report progress on a process that is already
 * running, such as ORDER_SHIPPED_EVENT. For every active process instance of the event's business entity, the
 * steps whose definitions list the event code are started or completed, and actual measurements are recorded.
 * <pre>
 * &#64;Component
 * &#64;EventHandler(eventType = "ORDER_SHIPPED_EVENT")
 * public class OrderShippedEventHandler extends AbstractMilestoneEventHandler {
 * }
 * </pre>
 *
 * @author arun kumar kandakatla
 */
@Slf4j
public abstract class AbstractMilestoneEventHandler implements EventHandler {

    @Autowired
    private ProcessInstanceService processInstanceService;
    @Autowired
    private StepProgressService stepProgressService;

    @Override
    public void handle(Event<?, ?> event) {
        final List<ProcessInstance> processInstances = processInstanceService
                .getActiveProcessInstances(event.getTenantKey(), entityType(event), entityId(event));
        if (processInstances.isEmpty()) {
            log.warn("No active process instance for {} {}; ignoring {}", entityType(event), entityId(event),
                    event.getEventCode());
            return;
        }
        final LocalDateTime occurredAt = occurredAt(event);
        processInstances.forEach(processInstance ->
                stepProgressService.recordMilestone(event.getEventCode(), processInstance, occurredAt));
    }

    /**
     * When the event happened in the business, used as the actual time of the steps it completes. Defaults to
     * {@link StepProgressService#occurredAt(Event)}; override to read it from the entity instead.
     */
    protected LocalDateTime occurredAt(Event<?, ?> event) {
        return StepProgressService.occurredAt(event);
    }

    protected String entityType(Event<?, ?> event) {
        return event.getEntityType();
    }

    protected String entityId(Event<?, ?> event) {
        return event.getEntityId();
    }
}
