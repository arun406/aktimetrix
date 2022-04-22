package com.aktimetrix.core.event.handler;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.api.ProcessType;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.api.Registry;
import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.exception.ProcessHandlerNotFoundException;
import com.aktimetrix.core.impl.DefaultProcessContext;
import com.aktimetrix.core.impl.DefaultProcessDefinitionProvider;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.transferobjects.Event;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractEventHandler implements EventHandler {
    private static final Logger logger = LoggerFactory.getLogger(AbstractEventHandler.class);
    private final Registry registry;
    private final DefaultProcessDefinitionProvider defaultProcessDefinitionProvider;

    /**
     * @param event
     */
    @Override
    public void handle(Event<?, ?> event) {
        logger.info("Entity Id : {}", event.getEntityId());
        // Query the Applicable Process Definitions based on the incoming event's event code.
        try {
            final List<ProcessDefinition> processDefinitions = getProcessDefinitions(event);
            // create instance for all process definitions
            if (!processDefinitions.isEmpty()) {
                processDefinitions.forEach(definition -> {
                    logger.info("process definition : {}", definition);
                    // should be changed to registry implementation.
                    Processor processHandler = null;
                    try {
                        processHandler = getProcessHandler(ProcessType.valueOf(definition.getProcessCode()));
                    } catch (ProcessHandlerNotFoundException e) {
                        logger.error("process handler is not defined for {} process", ProcessType.A2ATRANSPORT);
                        return;
                    }
                    // change this. TODO
                    DefaultProcessContext processContext = prepareProcessContext(definition, event);
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
    public List<ProcessDefinition> getProcessDefinitions(Event<?, ?> event) throws DefinitionNotFoundException {
        return defaultProcessDefinitionProvider.getDefinitions().stream()
                .filter(definition -> "CONFIRMED".equalsIgnoreCase(definition.getStatus()))
                .filter(definition -> definition.getStartEventCodes().contains(event.getEventCode()))
                .filter(definition -> event.getTenantKey().equalsIgnoreCase(definition.getTenant()))
                .collect(Collectors.toList());
    }

    /**
     * Returns the process Handler Based on Process Type
     *
     * @param processType event type
     * @return Event Handler Object
     * @throws ProcessHandlerNotFoundException
     */
    public Processor getProcessHandler(ProcessType processType) throws ProcessHandlerNotFoundException {
        final List<Object> handlers = this.registry
                .lookupAll(registryEntry -> registryEntry.hasAttribute(Constants.ATT_PROCESS_HANDLER_SERVICE) &&
                        registryEntry.attribute(Constants.ATT_PROCESS_HANDLER_SERVICE).equals(Constants.VAL_YES) &&
                        (ProcessType.valueOf((String) registryEntry.attribute(Constants.ATT_PROCESS_TYPE)) == processType)
                );
        logger.debug("applicable handlers {}", handlers);
        Processor processHandler = null;
        for (Object m : handlers) {
            processHandler = (Processor) m;
        }
        return processHandler;
    }

    /**
     * prepares the ProcessContext
     *
     * @param definition process definition
     * @param event      event
     * @return Process Context
     */
    public DefaultProcessContext prepareProcessContext(ProcessDefinition definition, Event<?, ?> event) {
        DefaultProcessContext processContext = new DefaultProcessContext();

        processContext.setProperty("entityId", entityId(event));
        processContext.setProperty("entityType", entityType(event));
        processContext.setProperty("event", event);
        processContext.setProperty("entity", event.getEntity());
        processContext.setProperty("eventData", event.getEventDetails());
        processContext.setTenant(event.getTenantKey());
        processContext.setProperty("processDefinition", definition);
        return processContext;
    }

    protected abstract String entityType(Event<?, ?> event);

    public abstract String entityId(Event<?, ?> event);
}
