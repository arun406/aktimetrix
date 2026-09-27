package com.aktimetrix.core.event.handler;

import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.exception.ProcessHandlerNotFoundException;
import com.aktimetrix.core.impl.DefaultContext;
import com.aktimetrix.core.impl.DefaultProcessor;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.service.StepProgressService;
import com.aktimetrix.core.transferobjects.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles a business event in two stages:
 * <ol>
 *     <li>starts every confirmed process of the tenant whose {@code startEventCodes} contain the event, using the
 *     {@code @ProcessHandler} registered for the process code, or {@link DefaultProcessor} when there is none;</li>
 *     <li>records the event as a milestone on every active process instance of the business entity, completing
 *     the steps whose definitions list it.</li>
 * </ol>
 * Events without an {@code @EventHandler} of their own are handled by {@link DefaultEventHandler}, so a subclass
 * is only needed to customise how the entity is identified.
 */
@Slf4j
public abstract class AbstractEventHandler implements EventHandler {

    @Autowired
    private ProcessDefinitionService processDefinitionService;
    @Autowired
    private RegistryService registryService;
    @Autowired
    private DefaultProcessor defaultProcessor;
    @Autowired
    private StepProgressService stepProgressService;

    @Override
    public void handle(Event<?, ?> event) {
        log.info("Event {} for {} {}", event.getEventCode(), entityType(event), entityId(event));
        startProcesses(event);
        stepProgressService.recordMilestones(event.getTenantKey(), entityType(event), entityId(event),
                event.getEventCode(), occurredAt(event));
    }

    private void startProcesses(Event<?, ?> event) {
        final List<ProcessDefinition> definitions = processDefinitionService.findStartedBy(event.getTenantKey(),
                event.getEventCode());
        for (ProcessDefinition definition : definitions) {
            log.info("Starting process {} for {} {}", definition.getProcessCode(), entityType(event), entityId(event));
            processHandler(definition).process(prepareContext(definition, event));
        }
    }

    private Processor processHandler(ProcessDefinition definition) {
        try {
            return registryService.getProcessHandler(definition.getProcessCode());
        } catch (ProcessHandlerNotFoundException e) {
            log.debug("No @ProcessHandler for {}; using the default processor", definition.getProcessCode());
            return defaultProcessor;
        }
    }

    public DefaultContext prepareContext(ProcessDefinition definition, Event<?, ?> event) {
        DefaultContext processContext = new DefaultContext();

        processContext.setProperty("entityId", entityId(event));
        processContext.setProperty("entityType", entityType(event));
        processContext.setProperty("event", event);
        processContext.setProperty("entity", event.getEntity());
        processContext.setProperty("eventData", event.getEventDetails());
        processContext.setTenant(event.getTenantKey());
        processContext.setProperty("processDefinition", definition);
        // pre- and post-processors are selected by the process type, which defaults to the process code
        processContext.setProcessType(definition.getProcessType() != null ? definition.getProcessType()
                : definition.getProcessCode());
        return processContext;
    }

    /**
     * When the event happened in the business. Override to read it from the entity instead of the event envelope.
     */
    protected LocalDateTime occurredAt(Event<?, ?> event) {
        return stepProgressService.occurredAt(event);
    }

    protected String entityType(Event<?, ?> event) {
        return event.getEntityType();
    }

    public String entityId(Event<?, ?> event) {
        return event.getEntityId();
    }
}
