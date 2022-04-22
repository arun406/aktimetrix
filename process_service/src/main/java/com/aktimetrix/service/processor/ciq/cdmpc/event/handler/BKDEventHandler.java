package com.aktimetrix.service.processor.ciq.cdmpc.event.handler;

import com.aktimetrix.service.processor.core.Constants;
import com.aktimetrix.service.processor.core.api.Processor;
import com.aktimetrix.service.processor.core.api.Registry;
import com.aktimetrix.service.processor.core.event.EventType;
import com.aktimetrix.service.processor.core.exception.DefinitionNotFoundException;
import com.aktimetrix.service.processor.core.exception.ProcessHandlerNotFoundException;
import com.aktimetrix.service.processor.core.impl.DefaultProcessContext;
import com.aktimetrix.service.processor.core.impl.DefaultProcessDefinitionProvider;
import com.aktimetrix.service.processor.core.process.ProcessType;
import com.aktimetrix.service.processor.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.service.processor.core.referencedata.service.ProcessDefinitionService;
import com.aktimetrix.service.processor.core.stereotypes.EventHandler;
import com.aktimetrix.service.processor.core.transferobjects.BKDEventDetails;
import com.aktimetrix.service.processor.core.transferobjects.Cargo;
import com.aktimetrix.service.processor.core.transferobjects.Event;
import com.aktimetrix.service.processor.core.transferobjects.Itinerary;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author arun kumar k
 */
@Component
@EventHandler(eventType = EventType.BKD)
@RequiredArgsConstructor
public class BKDEventHandler implements com.aktimetrix.service.processor.core.api.EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(BKDEventHandler.class);
    private final ProcessDefinitionService processDefinitionService;
    private final Registry registry;
    private final DefaultProcessDefinitionProvider defaultProcessDefinitionProvider;

    /**
     * This method handles the BKD Event
     *
     * @param event cargo booking event
     */
    @Override
    public void handle(Event<?, ?> event) {
        logger.info(" Entity Id : {}", event.getEntityId());
        // Query the Applicable Process Definitions based on the BKD event code.
        try {
            final List<ProcessDefinition> processDefinitions = getProcessDefinitions(event);

            // create instance for all process definitions
            if (!processDefinitions.isEmpty()) {
                processDefinitions.forEach(definition -> {
                    logger.info("Process Definition : {}", definition);
                    final Cargo cargo = (Cargo) event.getEntity();
                    final Itinerary itinerary = ((BKDEventDetails) event.getEventDetails()).getItineraries().get(0);
                    // should be changed to registry implementation.
                    Processor processHandler = null;
                    try {
                        processHandler = getProcessHandler(ProcessType.CDMP_C);
                    } catch (ProcessHandlerNotFoundException e) {
                        logger.error("process handler is not defined for {} process", ProcessType.CDMP_C);
                        return;
                    }
                    // change this. TODO
                    DefaultProcessContext processContext = prepareProcessContext(definition, cargo, itinerary, event);
                    processHandler.process(processContext);
                });
            }
        } catch (DefinitionNotFoundException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Returns the process definitions
     *
     * @param event event
     * @return Process Definition
     * @throws DefinitionNotFoundException
     */
    private List<ProcessDefinition> getProcessDefinitions(Event<?, ?> event) throws DefinitionNotFoundException {
        return defaultProcessDefinitionProvider.getDefinitions().stream()
                .filter(definition -> "CONFIRMED".equalsIgnoreCase(definition.getStatus()))
                .filter(definition -> definition.getStartEventCodes().contains(event.getEventCode()))
                .filter(definition -> event.getTenantKey().equalsIgnoreCase(definition.getTenant()))
                .collect(Collectors.toList());
    }


    /**
     * prepares the ProcessContext
     *
     * @param definition process definition
     * @param cargo      cargo
     * @param itinerary  itinerary
     * @param event      event
     * @return Process Context
     */
    private DefaultProcessContext prepareProcessContext(ProcessDefinition definition, Cargo cargo, Itinerary itinerary, Event<?, ?> event) {
        DefaultProcessContext processContext = new DefaultProcessContext();

        String entityId = cargo.getDocumentInfo().getAwbInfo().getDocumentPrefix()
                + "-" + cargo.getDocumentInfo().getAwbInfo().getDocumentNumber();
        logger.info("entity id: {}", entityId);

        processContext.setProperty("entityId", entityId);
        processContext.setProperty("entityType", "cargo.awb");
        processContext.setProperty("event", event);
        processContext.setProperty("entity", cargo);
        processContext.setProperty("eventData", itinerary);
        processContext.setTenant(event.getTenantKey());
        processContext.setProperty("processDefinition", definition);
        return processContext;
    }


    /**
     * Returns the process Handler Based on Process Type
     *
     * @param processType event type
     * @return Event Handler Object
     * @throws ProcessHandlerNotFoundException
     */
    private Processor getProcessHandler(ProcessType processType) throws ProcessHandlerNotFoundException {
        final List<Object> handlers = this.registry
                .lookupAll(registryEntry -> registryEntry.hasAttribute(Constants.ATT_PROCESS_HANDLER_SERVICE) &&
                        registryEntry.attribute(Constants.ATT_PROCESS_HANDLER_SERVICE).equals(Constants.VAL_YES) &&
                        (ProcessType.valueOf((String) registryEntry.attribute(Constants.ATT_PROCESS_TYPE)) == processType)
                );
        logger.debug("Applicable handlers {}", handlers);
        Processor processHandler = null;
        for (Object m : handlers) {
            processHandler = (Processor) m;
        }
        return processHandler;
    }
}
