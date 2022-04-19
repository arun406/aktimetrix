package com.aktimetrix.service.processor;

import com.aktimetrix.service.processor.core.Constants;
import com.aktimetrix.service.processor.core.api.EventHandler;
import com.aktimetrix.service.processor.core.api.Registry;
import com.aktimetrix.service.processor.core.event.EventType;
import com.aktimetrix.service.processor.core.exception.EventHandlerNotFoundException;
import com.aktimetrix.service.processor.core.exception.ProcessorException;
import com.aktimetrix.service.processor.core.impl.RegistryEntry;
import com.aktimetrix.service.processor.core.transferobjects.BKDEventDetails;
import com.aktimetrix.service.processor.core.transferobjects.Cargo;
import com.aktimetrix.service.processor.core.transferobjects.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ComponentScan("com.aktimetrix.service.processor")
@SpringBootApplication
@RequiredArgsConstructor
public class ProcessManagerApplication {
    final private static Logger logger = LoggerFactory.getLogger(ProcessManagerApplication.class);
    private final ObjectMapper objectMapper;
    private final Registry registry;

    /**
     * @param args
     */
    public static void main(String[] args) {
        SpringApplication.run(ProcessManagerApplication.class, args);
    }

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
                logger.info("Event Code: " + eventCode + ", Event Type: " + eventType);

                final Event<Cargo, BKDEventDetails> event = objectMapper.readValue(payload, new TypeReference<>() {
                });
                logger.info(String.format("Event : %s ", event));
                EventHandler eventHandler = getEventHandler(EventType.valueOf(eventCode));
                eventHandler.handle(event);
            } catch (JsonProcessingException | EventHandlerNotFoundException e) {
                logger.error("Something happened bad. please contact system administrator.", e);
            }
        };
    }

    /**
     * Returns the Event Handler Based on Event Type
     *
     * @param eventType event type
     * @return Event Handler Object
     * @throws EventHandlerNotFoundException
     */
    private EventHandler getEventHandler(EventType eventType) throws EventHandlerNotFoundException {
        Predicate<RegistryEntry> predicate1 = re -> re.hasAttribute(Constants.ATT_EVENT_HANDLER_SERVICE);
        Predicate<RegistryEntry> predicate2 = re -> re.attribute(Constants.ATT_EVENT_HANDLER_SERVICE).equals(Constants.VAL_YES);
        Predicate<RegistryEntry> predicate3 = re -> (EventType.valueOf((String) re.attribute(Constants.ATT_EVENT_TYPE)) == eventType);

        final List<Object> eventHandlers = this.registry.lookupAll(predicate1.and(predicate2).and(predicate3));
        logger.debug("Applicable eventHandlers {}", eventHandlers);
        if (eventHandlers == null || eventHandlers.isEmpty()) {
            throw new EventHandlerNotFoundException(String.format("event handlers not found for %s", eventType));
        }
        EventHandler eventHandler = null;
        for (Object m : eventHandlers) {
            eventHandler = (EventHandler) m;
        }
        return eventHandler;
    }
}

