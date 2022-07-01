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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public abstract class AbstractStepEventHandler implements EventHandler {

    final private static Logger logger = LoggerFactory.getLogger(AbstractStepEventHandler.class);
    final private RegistryService registryService;

    @Value("${aktimetrix.meter.process-type:DEFAULT_PROCESS_TYPE}")
    private String processType;

    @Value("${aktimetrix.meter.process-code:DEFAULT_PROCESS_CODE}")
    private String processCode;

    /**
     * @param event
     */
    @Override
    public void handle(Event<?, ?> event) {
        logger.info("Entity Id : {}", event.getEntityId());
        try {
            logger.debug("looking for process handler for {}-{}", Constants.DEFAULT_PROCESS_TYPE, Constants.DEFAULT_PROCESS_CODE);
            Processor processor = registryService.getProcessHandler(Constants.DEFAULT_PROCESS_TYPE, Constants.DEFAULT_PROCESS_CODE);
            DefaultContext context = prepareContext(event);
            processor.process(context);
        } catch (ProcessHandlerNotFoundException | MultipleProcessHandlersFoundException e) {
            logger.error("process handler is not defined for {} - {} process", processType, processCode);
        }
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
