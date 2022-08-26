package com.aktimetrix.core.event.handler;

import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.api.ProcessType;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.exception.MultipleProcessHandlersFoundException;
import com.aktimetrix.core.exception.ProcessHandlerNotFoundException;
import com.aktimetrix.core.impl.DefaultContext;
import com.aktimetrix.core.impl.DefaultProcessDefinitionProvider;
import com.aktimetrix.core.impl.DefaultStepDefinitionProvider;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.transferobjects.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public abstract class AbstractEventHandler implements EventHandler {

    private final RegistryService registryService;
    private final DefaultProcessDefinitionProvider processDefinitionProvider;
    private final DefaultStepDefinitionProvider stepDefinitionProvider;

    @Autowired
    public AbstractEventHandler(RegistryService registryService, DefaultProcessDefinitionProvider processDefinitionProvider,
                                DefaultStepDefinitionProvider stepDefinitionProvider) {
        this.registryService = registryService;
        this.processDefinitionProvider = processDefinitionProvider;
        this.stepDefinitionProvider = stepDefinitionProvider;
    }

    /**
     * event handler
     *
     * @param event
     */
    @Override
    public void handle(Event<?, ?> event) {
        log.info("Entity Id : {}", event.getEntityId());
        // Query the Applicable Process Definitions based on the incoming event's event code.
        final List<ProcessDefinition> processDefinitions = getProcessDefinitions(event.getTenantKey(), event.getEventCode());
        if (processDefinitions != null && !processDefinitions.isEmpty()) {
            handleProcessEvents(event, processDefinitions);
        }

        final List<StepDefinition> stepDefinitions = getStepDefinitions(event.getTenantKey(), event.getEventCode());
        if (stepDefinitions != null && !stepDefinitions.isEmpty()) {
            handleStepEvents(event, stepDefinitions);
        }
    }

    /**
     * execute the step handlers based on the step code
     *
     * @param event
     * @param stepDefinitions
     */
    private void handleStepEvents(Event<?, ?> event, List<StepDefinition> stepDefinitions) {
        stepDefinitions.forEach(definition -> {
            log.info("step definition: {}", definition);
            try {
                Processor processor = this.registryService
                        .getProcessors("CiQ", definition.getStepCode());
                DefaultContext processContext = prepareContext(definition, event);
                processor.process(processContext);
            } catch (ProcessHandlerNotFoundException | MultipleProcessHandlersFoundException e) {
                log.error("process handler is not defined for {} process", ProcessType.A2ATRANSPORT);
                return;
            }
        });
    }

    private List<StepDefinition> getStepDefinitions(String tenantKey, String eventCode) {
        return stepDefinitionProvider.getDefinitions(tenantKey, eventCode);
    }

    /**
     * @param event
     * @param processDefinitions
     */
    private void handleProcessEvents(Event<?, ?> event, List<ProcessDefinition> processDefinitions) {
        processDefinitions.forEach(definition -> {
            log.info("process definition : {}", definition);
            try {
                Processor processor = this.registryService.getProcessors(definition.getProcessType(), definition.getProcessCode());
                DefaultContext processContext = prepareContext(definition, event);
                processor.process(processContext);
            } catch (ProcessHandlerNotFoundException | MultipleProcessHandlersFoundException e) {
                log.error("process handler is not defined for {} process", ProcessType.A2ATRANSPORT);
                return;
            }
        });
    }

    /**
     * Returns the process definitions
     *
     * @param tenantKey
     * @param eventCode
     * @return Process Definition
     * @throws DefinitionNotFoundException
     */
    public List<ProcessDefinition> getProcessDefinitions(String tenantKey, String eventCode) {
        return this.processDefinitionProvider.getDefinitions(tenantKey, eventCode);
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
        processContext.setProcessCode(definition.getProcessCode());
        return processContext;
    }

    /**
     * prepares the ProcessContext
     *
     * @param definition process definition
     * @param event      event
     * @return Process Context
     */
    public DefaultContext prepareContext(StepDefinition definition, Event<?, ?> event) {
        DefaultContext context = new DefaultContext();

        context.setProperty("entityId", entityId(event));
        context.setProperty("entityType", entityType(event));
        context.setProperty("event", event);
        context.setProperty("entity", event.getEntity());
        context.setProperty("eventData", event.getEventDetails());
        context.setTenant(event.getTenantKey());
        context.setProperty("stepDefinition", definition);
        context.setProperty("stepCode", definition.getStepCode());
        context.setProcessType(definition.getProcessType());
        context.setProcessCode(definition.getProcessCode());
        return context;
    }

    protected String entityType(Event<?, ?> event) {
        return event.getEntityType();
    }

    public String entityId(Event<?, ?> event) {
        return event.getEntityId();
    }
}
