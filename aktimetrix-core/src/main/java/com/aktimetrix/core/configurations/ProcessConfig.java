package com.aktimetrix.core.configurations;

import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.api.Registry;
import com.aktimetrix.core.exception.EventHandlerNotFoundException;
import com.aktimetrix.core.exception.MultipleEventHandlersFoundException;
import com.aktimetrix.core.exception.ProcessorException;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.transferobjects.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
public class ProcessConfig {

    final private static Logger logger = LoggerFactory.getLogger(ProcessConfig.class);
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Registry registry;
    @Autowired
    private RegistryService registryService;

    @Bean
    public Consumer<Message<String>> processor() throws ProcessorException {
        return message -> {
            final String payload = message.getPayload();
            final Acknowledgment acknowledgment = message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
            logger.debug("payload: {}", payload);
            final JsonNode node;
            try {
                node = objectMapper.readTree(payload);
                String eventCode = node.get("eventCode").asText();
                String eventType = node.get("eventType").asText();
                logger.info("Event Code: {}, Event Type: {}", eventCode, eventType);

                final Event<?, ?> event = objectMapper.readValue(payload, new TypeReference<Event>() {
                });
                logger.info(String.format("Event : %s ", event));
                EventHandler eventHandler = registryService.getEventHandler(eventType, eventCode);
                eventHandler.handle(event);
            } catch (JsonProcessingException | EventHandlerNotFoundException | MultipleEventHandlersFoundException e) {
                logger.error("Something happened bad. please contact system administrator.", e);
            }
        };
    }
}
