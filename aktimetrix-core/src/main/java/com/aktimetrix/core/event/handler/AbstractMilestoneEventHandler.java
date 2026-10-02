package com.aktimetrix.core.event.handler;

import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.service.StepProgressService;
import com.aktimetrix.core.transferobjects.Event;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

/**
 * Base class for handlers of milestone events that never start a process: for every active process instance of
 * the event's business entity, the steps whose definitions list the event code are started or completed, and
 * actual measurements are recorded.
 * <p>
 * Events without an {@code @EventHandler} are already handled this way by {@link DefaultEventHandler}; extend this
 * class only to customise how the entity or the event time is read.
 * <pre>
 * &#64;Component
 * &#64;EventHandler(eventType = "ORDER_SHIPPED_EVENT")
 * public class OrderShippedEventHandler extends AbstractMilestoneEventHandler {
 * }
 * </pre>
 *
 * @author arun kumar kandakatla
 */
public abstract class AbstractMilestoneEventHandler implements EventHandler {

    @Autowired
    private StepProgressService stepProgressService;

    @Override
    public void handle(Event<?, ?> event) {
        stepProgressService.recordMilestones(event.getTenantKey(), entityType(event), entityId(event),
                event.getEventCode(), occurredAt(event), event);
    }

    /**
     * When the event happened in the business, used as the actual time of the steps it completes. Defaults to
     * {@link StepProgressService#occurredAt(Event)}; override to read it from the entity instead.
     */
    protected Instant occurredAt(Event<?, ?> event) {
        return stepProgressService.occurredAt(event);
    }

    protected String entityType(Event<?, ?> event) {
        return event.getEntityType();
    }

    protected String entityId(Event<?, ?> event) {
        return event.getEntityId();
    }
}
