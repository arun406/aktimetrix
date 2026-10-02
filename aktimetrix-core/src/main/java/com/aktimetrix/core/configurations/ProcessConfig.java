package com.aktimetrix.core.configurations;

import com.aktimetrix.autoconfigure.AktimetrixDefaultProperties;
import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.api.EventMapper;
import com.aktimetrix.core.event.handler.DefaultEventHandler;
import com.aktimetrix.core.exception.EventHandlerNotFoundException;
import com.aktimetrix.core.exception.MultipleEventHandlerFoundException;
import com.aktimetrix.core.outbox.Outbox;
import com.aktimetrix.core.service.AktimetrixMetrics;
import com.aktimetrix.core.service.ProcessingContext;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.service.StepProgressService;
import com.aktimetrix.core.store.AktimetrixTransactions;
import com.aktimetrix.core.store.ProcessedEventStore;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.EventContext.Cause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Scheduled;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * Consumes the inbound business events (binding {@code processor-in-0}, topic {@code aktimetrix.events.topic}) and
 * routes each to the {@code @EventHandler} registered for its event code, or to {@link DefaultEventHandler}.
 */
@Configuration
public class ProcessConfig {

    final private static Logger logger = LoggerFactory.getLogger(ProcessConfig.class);
    @Autowired
    private EventMapper eventMapper;
    @Autowired
    private RegistryService registryService;
    @Autowired
    private DefaultEventHandler defaultEventHandler;
    @Autowired
    private AktimetrixMetrics metrics;
    @Autowired
    private AktimetrixTransactions transactions;
    @Autowired
    private Outbox outbox;
    @Autowired
    private AktimetrixProperties properties;
    @Autowired
    private Clock clock;
    @Autowired
    private ProcessedEventStore processedEvents;

    @Bean
    public Consumer<Message<?>> processor() {
        return message -> {
            final String payload = text(message.getPayload());
            logger.debug("payload: {}", payload);
            final Event<?, ?> event;
            try {
                event = eventMapper.map(payload, message.getHeaders());
            } catch (Exception e) {
                reject(null, payload, "cannot be read: " + e.getMessage());
                return;
            }
            if (event == null) {
                logger.debug("Event ignored by the event mapper: {}", payload);
                metrics.eventReceived(null, null, "ignored");
                return;
            }
            if (event.getEventCode() == null || event.getTenantKey() == null || event.getEntityId() == null) {
                reject(event, payload, "has no eventCode, tenantKey or entityId");
                return;
            }
            final Instant occurredAt = StepProgressService.occurredAt(event, clock);
            if (occurredAt.isAfter(clock.instant().plus(properties.getEvents().getMaxFutureSkew()))) {
                reject(event, payload, "is dated in the future, " + occurredAt);
                return;
            }
            final boolean deduplicate = properties.getEvents().getDeduplication().isEnabled()
                    && event.getEventId() != null;
            if (deduplicate && processedEvents.isProcessed(event.getTenantKey(), event.getEventId())) {
                duplicate(event);
                return;
            }
            try {
                // every event published while handling it records it as their cause, and its business time
                final Cause cause = new Cause(Cause.EVENT, event.getEventId(), event.getEventCode());
                ProcessingContext.run(cause, occurredAt, () -> transactions.run(() -> {
                    eventHandler(event.getEventCode()).handle(event);
                    if (deduplicate) {
                        processedEvents.markProcessed(event.getTenantKey(), event.getEventId(), clock.instant());
                    }
                }));
                metrics.eventReceived(event.getTenantKey(), event.getEventCode(), "handled");
            } catch (DuplicateKeyException e) {
                // processed by another instance at the same time; with an atomic store, this processing is undone
                duplicate(event);
            } catch (RuntimeException e) {
                metrics.eventReceived(event.getTenantKey(), event.getEventCode(), "failed");
                throw e;
            }
        };
    }

    /**
     * The payload as text: brokers deliver bytes, which are read as UTF-8.
     */
    private static String text(Object payload) {
        return payload instanceof byte[] ? new String((byte[]) payload, StandardCharsets.UTF_8) : String.valueOf(payload);
    }

    private EventHandler eventHandler(String eventCode) {
        try {
            return registryService.getEventHandler(eventCode);
        } catch (EventHandlerNotFoundException e) {
            return defaultEventHandler;
        } catch (MultipleEventHandlerFoundException e) {
            throw new IllegalStateException("More than one @EventHandler is registered for " + eventCode, e);
        }
    }

    private void duplicate(Event<?, ?> event) {
        logger.info("Event {} was already processed; ignored", safe(event.getEventId()));
        metrics.eventReceived(event.getTenantKey(), event.getEventCode(), "duplicate");
    }

    /**
     * Forgets processed event ids older than {@code aktimetrix.events.deduplication.retention}.
     */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1H")
    public void forgetProcessedEvents() {
        processedEvents.deleteProcessedBefore(clock.instant().minus(properties.getEvents().getDeduplication().getRetention()));
    }

    /**
     * Identifies a rejected event for the log without its content, which may hold personal data and, being untrusted,
     * line breaks that would forge log lines; the content goes to the dead-letter topic, and to the log at DEBUG.
     */
    private static String describe(Event<?, ?> event, String payload) {
        final int size = payload == null ? 0 : payload.length();
        if (event == null) {
            return "unreadable payload of " + size + " characters";
        }
        return String.format("eventId=%s, eventCode=%s, entityId=%s, %d characters", safe(event.getEventId()),
                safe(event.getEventCode()), safe(event.getEntityId()), size);
    }

    private static String safe(String value) {
        if (value == null) {
            return null;
        }
        final String printable = value.replaceAll("\\p{Cntrl}", "?");
        return printable.length() > 100 ? printable.substring(0, 100) + "…" : printable;
    }

    /**
     * An event that can never be processed: retrying cannot help, so it goes straight to the dead-letter topic.
     */
    private void reject(Event<?, ?> event, String payload, String reason) {
        metrics.eventReceived(event == null ? null : event.getTenantKey(), event == null ? null : event.getEventCode(),
                "invalid");
        if (properties.getEvents().getDeadLetter().isEnabled()) {
            logger.error("Event {}; sent to the dead-letter topic: {}", reason, describe(event, payload));
            logger.debug("Rejected payload: {}", payload);
            outbox.enqueueRaw(AktimetrixDefaultProperties.DEAD_LETTER_BINDING,
                    event == null ? null : event.getEntityId(), payload);
        } else {
            logger.error("Event {}; ignored: {}", reason, describe(event, payload));
            logger.debug("Rejected payload: {}", payload);
        }
    }
}
