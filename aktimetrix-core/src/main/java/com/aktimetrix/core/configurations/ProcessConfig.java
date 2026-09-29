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
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.EventContext.Cause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
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
            try {
                // every event published while handling it records it as their cause, and its business time
                final Cause cause = new Cause(Cause.EVENT, event.getEventId(), event.getEventCode());
                ProcessingContext.run(cause, StepProgressService.occurredAt(event, clock),
                        () -> transactions.run(() -> eventHandler(event.getEventCode()).handle(event)));
                metrics.eventReceived(event.getTenantKey(), event.getEventCode(), "handled");
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

    /**
     * An event that can never be processed: retrying cannot help, so it goes straight to the dead-letter topic.
     */
    private void reject(Event<?, ?> event, String payload, String reason) {
        metrics.eventReceived(event == null ? null : event.getTenantKey(), event == null ? null : event.getEventCode(),
                "invalid");
        if (properties.getEvents().getDeadLetter().isEnabled()) {
            logger.error("Event {}; sent to the dead-letter topic: {}", reason, payload);
            outbox.enqueueRaw(AktimetrixDefaultProperties.DEAD_LETTER_BINDING,
                    event == null ? null : event.getEntityId(), payload);
        } else {
            logger.error("Event {}; ignored: {}", reason, payload);
        }
    }
}
