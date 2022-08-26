package com.aktimetrix.core.event.handler;


import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.exception.MultipleProcessHandlersFoundException;
import com.aktimetrix.core.exception.ProcessHandlerNotFoundException;
import com.aktimetrix.core.impl.DefaultContext;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.StepInstanceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractStepEventHandler implements EventHandler {

    @Autowired
    public RegistryService registryService;

    /**
     * @param event
     */
    @Override
    public void handle(Event<?, ?> event) {
        log.info("Entity Id : {}", event.getEntityId());
        try {
            String processType = getProcessType(event);
            String processCode = getProcessCode(event);
            log.debug("looking for process handler for {}-{}", processType, processCode);
            Processor processor = this.registryService.getProcessors(processType, processCode);
            DefaultContext context = prepareContext(event);
            processor.process(context);
        } catch (ProcessHandlerNotFoundException | MultipleProcessHandlersFoundException e) {
            log.error("error while getting the process handlers {}", e.getMessage());
        }
    }

    public String getProcessType(Event<?, ?> event) {
        if (Constants.STEP_EVENT.equals(event.getEventType())) {
            return Constants.MEASUREMENT_INSTANCE_TYPE;
        }
        return null;
    }

    public String getProcessCode(Event<?, ?> event) {
        String processCode = null;
        switch (event.getEventCode()) {
            case Constants.STEP_CREATED:
            case Constants.STEP_COMPLETED:
                processCode = Constants.MEASUREMENT_INSTANCE_CREATE;
                break;
            case Constants.STEP_CANCELLED:
                processCode = Constants.MEASUREMENT_INSTANCE_CANCEL;
                break;
        }
        return processCode;
    }


    /**
     * prepares the ProcessContext
     *
     * @param event event
     * @return Process Context
     */
    public DefaultContext prepareContext(Event<?, ?> event) {
        DefaultContext processContext = new DefaultContext();
        processContext.setProperty("entityId", entityId(event));
        processContext.setProperty("entityType", entityType(event));
        processContext.setProperty("event", event);
        final StepInstanceDTO entity = (StepInstanceDTO) event.getEntity();
        processContext.setProperty("entity", entity);
        processContext.setProperty("eventData", event.getEventDetails());
        processContext.setTenant(event.getTenantKey());
        List<StepInstance> stepInstances = new ArrayList<>();
        stepInstances.add(getStepInstance(entity));
        processContext.setStepInstances(stepInstances);
        return processContext;
    }


    /**
     * prepares the step instance
     *
     * @param dto
     * @return
     */
    private StepInstance getStepInstance(StepInstanceDTO dto) {
        StepInstance instance = new StepInstance();
        instance.setId(dto.getId());
        instance.setProcessInstanceId(dto.getProcessInstanceId());
        instance.setCreatedOn(dto.getCreatedOn());
        instance.setFunctionalCtx(dto.getFunctionalCtx());
        instance.setGroupCode(dto.getGroupCode());
        instance.setStepCode(dto.getStepCode());
        instance.setLocationCode(dto.getLocationCode());
        instance.setMetadata(dto.getMetadata());
        instance.setStatus(dto.getStatus());
        instance.setTenant(dto.getTenant());
        instance.setVersion(dto.getVersion());
        return instance;
    }

    protected String entityType(Event<?, ?> event) {
        return event.getEntityId();
    }

    protected String entityId(Event<?, ?> event) {
        return event.getEntityId();
    }
}
