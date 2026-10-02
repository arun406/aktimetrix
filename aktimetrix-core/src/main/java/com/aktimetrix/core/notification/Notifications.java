package com.aktimetrix.core.notification;

import com.aktimetrix.core.api.Notification;
import com.aktimetrix.core.api.Notifier;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.outbox.Outbox;
import com.aktimetrix.core.outbox.OutboxMessage;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.EventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.stream.Collectors;

/**
 * Turns the published changes that need attention into {@link Notification}s for the application's {@link Notifier}s:
 * a step or process going {@code AT_RISK} or {@code OVERDUE}, or completing {@code LATE}, as chosen by
 * {@code aktimetrix.notifications.on}. They are queued in the outbox, in the unit of work of the change, and the
 * outbox relay hands them to the notifiers; nothing is queued when there is no notifier.
 */
@Component
public class Notifications {
    private static final Logger logger = LoggerFactory.getLogger(Notifications.class);

    /**
     * Outbox destination of notifications, delivered to the notifiers rather than to the message broker.
     */
    public static final String DESTINATION = "aktimetrix-notifications";
    static final String AT_RISK = "AT_RISK";
    static final String OVERDUE = "OVERDUE";
    static final String LATE = "LATE";

    private final Outbox outbox;
    private final ObjectProvider<Notifier> notifiers;
    private final AktimetrixProperties properties;
    private final ObjectMapper objectMapper;

    public Notifications(Outbox outbox, ObjectProvider<Notifier> notifiers, AktimetrixProperties properties,
                         ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.notifiers = notifiers;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Queues a notification for a published step change, when it needs attention.
     */
    public void onStep(StepInstance step, String eventCode, Event<?, EventContext> published) {
        final String condition = condition(eventCode, step.getTimeliness());
        if (!wanted(condition)) {
            return;
        }
        final EventContext context = published.getEventDetails();
        queue(Notification.builder().id(published.getEventId()).subject(Notification.STEP).condition(condition)
                .tenant(step.getTenant()).processCode(context == null ? null : context.getProcessCode())
                .processInstanceId(step.getProcessInstanceId()).entityType(entityType(context))
                .entityId(entityId(context)).stepCode(step.getStepCode()).stepInstanceId(step.getId())
                .plannedAt(step.getPlannedAt()).lateAfter(step.getLateAfter()).expectedAt(step.getExpectedAt())
                .actualAt(step.getActualAt()).occurredAt(context == null ? null : context.getOccurredAt()).build(),
                step.getProcessInstanceId());
    }

    /**
     * Queues a notification for a published process change, when it needs attention.
     */
    public void onProcess(ProcessInstance process, String eventCode, Event<?, EventContext> published) {
        final String condition = condition(eventCode, process.getTimeliness());
        if (!wanted(condition)) {
            return;
        }
        final EventContext context = published.getEventDetails();
        queue(Notification.builder().id(published.getEventId()).subject(Notification.PROCESS).condition(condition)
                .tenant(process.getTenant()).processCode(process.getProcessCode()).processInstanceId(process.getId())
                .run(process.getRun()).entityType(process.getEntityType()).entityId(process.getEntityId())
                .plannedAt(process.getPlannedAt()).lateAfter(process.getLateAfter()).actualAt(process.getEndedAt())
                .occurredAt(context == null ? null : context.getOccurredAt()).build(), process.getId());
    }

    /**
     * Hands a queued notification to every notifier.
     *
     * @throws Exception when a notifier fails; the notification is delivered again later
     */
    public void deliver(OutboxMessage message) throws Exception {
        final Notification notification = objectMapper.readValue(message.getPayload(), Notification.class);
        for (Notifier notifier : notifiers.orderedStream().collect(Collectors.toList())) {
            notifier.notify(notification);
        }
    }

    private static String condition(String eventCode, Timeliness timeliness) {
        if (AT_RISK.equals(eventCode) || OVERDUE.equals(eventCode)) {
            return eventCode;
        }
        return "COMPLETED".equals(eventCode) && timeliness == Timeliness.LATE ? LATE : null;
    }

    private boolean wanted(String condition) {
        return condition != null && properties.getNotifications().getOn().contains(condition)
                && notifiers.orderedStream().findAny().isPresent();
    }

    private void queue(Notification notification, String key) {
        logger.debug("Notification {} {} of {} {}", notification.getCondition(), notification.getSubject(),
                notification.getEntityType(), notification.getEntityId());
        outbox.enqueue(DESTINATION, key, notification);
    }

    private static String entityType(EventContext context) {
        return context == null || context.getBusinessEntity() == null ? null : context.getBusinessEntity().getEntityType();
    }

    private static String entityId(EventContext context) {
        return context == null || context.getBusinessEntity() == null ? null : context.getBusinessEntity().getEntityId();
    }
}
