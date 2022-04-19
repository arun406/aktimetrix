package com.aktimetrix.service.processor.core.resource;

import com.aktimetrix.service.processor.core.Constants;
import com.aktimetrix.service.processor.core.api.EventHandler;
import com.aktimetrix.service.processor.core.api.Registry;
import com.aktimetrix.service.processor.core.event.EventType;
import com.aktimetrix.service.processor.core.exception.EventHandlerNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RequestMapping("/svm/process-instances/")
@RestController
@RequiredArgsConstructor
public class EventProcessorResource {

    private final Logger logger = LoggerFactory.getLogger(EventProcessorResource.class);

    private final ObjectMapper objectMapper;
    private final Registry registry;

    @PostMapping("/actions/create")
    public ResponseEntity<Object> process(@RequestBody String input) {
        /*try {
            final JsonNode node = objectMapper.readTree(input);
            String eventCode = node.get("eventCode").asText();
            String eventType = node.get("eventType").asText();
            logger.info("Event Code: " + eventCode + ", Event Type: " + eventType);

            final Event<Cargo, BKDEventDetails> bkdEvent = objectMapper.readValue(input, new TypeReference<>() {
            });
            logger.info(String.format(" BKD Event : %s ", bkdEvent));
            EventHandler eventHandler
                    = getEventHandler(EventType.valueOf(eventCode));
            if (eventHandler != null) {
                eventHandler.handle(bkdEvent);
            }
        } catch (JsonProcessingException e) {
            logger.error("Exception Occurred", e);
        } catch (EventHandlerNotFoundException e) {
            logger.error(e.getMessage(), e);
        }*/
        return ResponseEntity.created(URI.create("/svm/process-instance/")).build();
    }

    /**
     * @param eventType
     * @return
     * @throws EventHandlerNotFoundException
     */
    private EventHandler getEventHandler(EventType eventType) throws EventHandlerNotFoundException {
        final List<Object> eventHandlers = this.registry.lookupAll(registryEntry -> registryEntry.hasAttribute(Constants.ATT_EVENT_HANDLER_SERVICE) &&
                registryEntry.attribute(Constants.ATT_EVENT_HANDLER_SERVICE).equals(Constants.VAL_YES) &&
                (EventType.valueOf((String) registryEntry.attribute(Constants.ATT_EVENT_TYPE)) == eventType)
        );
        logger.debug("Applicable eventHandlers {}", eventHandlers);
        EventHandler eventHandler = null;
        for (Object m : eventHandlers) {
            eventHandler = (EventHandler) m;
        }
        return eventHandler;
    }
}
