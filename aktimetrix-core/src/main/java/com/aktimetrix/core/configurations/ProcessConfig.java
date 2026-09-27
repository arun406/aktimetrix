package com.aktimetrix.core.configurations;

import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.event.handler.DefaultEventHandler;
import com.aktimetrix.core.exception.EventHandlerNotFoundException;
import com.aktimetrix.core.exception.MultipleEventHandlerFoundException;
import com.aktimetrix.core.service.AktimetrixMetrics;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.transferobjects.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * Consumes the inbound business events (binding {@code processor-in-0}, topic {@code aktimetrix.events.topic}) and
 * routes each to the {@code @EventHandler} registered for its event code, or to {@link DefaultEventHandler}.
 */
@Configuration
public class ProcessConfig {

    final private static Logger logger = LoggerFactory.getLogger(ProcessConfig.class);
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RegistryService registryService;
    @Autowired
    private DefaultEventHandler defaultEventHandler;
    @Autowired
    private AktimetrixMetrics metrics;

    @Bean
    public Consumer<Message<String>> processor() {
        return message -> {
            final String payload = message.getPayload();
            logger.debug("payload: {}", payload);
            final Event<?, ?> event;
            try {
                event = objectMapper.readValue(payload, new TypeReference<Event<Object, Object>>() {
                });
            } catch (JsonProcessingException e) {
                logger.error("Ignoring an event that is not valid JSON: {}", e.getOriginalMessage());
                metrics.eventReceived(null, null, "invalid");
                return;
            }
            if (event.getEventCode() == null || event.getTenantKey() == null || event.getEntityId() == null) {
                logger.error("Ignoring an event without eventCode, tenantKey or entityId: {}", payload);
                metrics.eventReceived(event.getTenantKey(), event.getEventCode(), "invalid");
                return;
            }
            try {
                eventHandler(event.getEventCode()).handle(event);
                metrics.eventReceived(event.getTenantKey(), event.getEventCode(), "handled");
            } catch (RuntimeException e) {
                metrics.eventReceived(event.getTenantKey(), event.getEventCode(), "failed");
                throw e;
            }
        };
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
}
