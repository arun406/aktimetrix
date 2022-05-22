package com.aktimetrix.core.event.handler;

import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.api.ProcessType;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.exception.ProcessHandlerNotFoundException;
import com.aktimetrix.core.impl.DefaultContext;
import com.aktimetrix.core.impl.DefaultProcessDefinitionProvider;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.transferobjects.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractEventHandler implements EventHandler {

    @Autowired
    private DefaultProcessDefinitionProvider defaultProcessDefinitionProvider;
    @Autowired
    private RegistryService registryService;

    /**
     * @param event
     */
    @Override
    public void handle(Event<?, ?> event) {
        log.info("Entity Id : {}", event.getEntityId());
        // Query the Applicable Process Definitions based on the incoming event's event code.
        try {
            final List<ProcessDefinition> processDefinitions = getProcessDefinitions(event);
            // create instance for all process definitions
            if (!processDefinitions.isEmpty()) {
                processDefinitions.forEach(definition -> {
                    log.info("process definition : {}", definition);
                    // should be changed to registry implementation.
                    Processor processHandler = null;
                    try {
                        processHandler = registryService.getProcessHandler(definition.getProcessCode());
                    } catch (ProcessHandlerNotFoundException e) {
                        log.error("process handler is not defined for {} process", ProcessType.A2ATRANSPORT);
                        return;
                    }
                    DefaultContext processContext = prepareContext(definition, event);
                    processHandler.process(processContext);
                });
            }
        } catch (DefinitionNotFoundException e) {
            log.error(e.getMessage(), e);
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
     * prepares the ProcessContext
     *
     * @param definition process definition
     * @param event      event
     * @return Process Context
     */
    public DefaultContext prepareContext(ProcessDefinition definition, Event<?, ?> event) {
        DefaultContext processContext = new DefaultContext();

        processContext.setProperty("entityId", entityId(event));
        processContext.setProperty("entityType", entityType(event));
        processContext.setProperty("event", event);
        processContext.setProperty("entity", event.getEntity());
        processContext.setProperty("eventData", event.getEventDetails());
        processContext.setTenant(event.getTenantKey());
        processContext.setProperty("processDefinition", definition);
        processContext.setProcessType(definition.getProcessType());
        return processContext;
    }

    protected String entityType(Event<?, ?> event) {
        return event.getEntityType();
    }

    public String entityId(Event<?, ?> event) {
        return event.getEntityId();
    }
}
