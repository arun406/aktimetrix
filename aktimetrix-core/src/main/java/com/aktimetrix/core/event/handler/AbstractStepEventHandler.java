package com.aktimetrix.core.event.handler;


import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.api.ProcessType;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.exception.ProcessHandlerNotFoundException;
import com.aktimetrix.core.impl.DefaultContext;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.StepInstanceDTO;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public abstract class AbstractStepEventHandler implements EventHandler {

    final private static Logger logger = LoggerFactory.getLogger(AbstractStepEventHandler.class);
    final private RegistryService registryService;

    /**
     * @param event
     */
    @Override
    public void handle(Event<?, ?> event) {
        logger.info("Entity Id : {}", event.getEntityId());

        try {
            Processor processor = registryService.getProcessHandler("METERPROCESSOR");
            DefaultContext context = prepareContext(event);
            processor.process(context);

        } catch (ProcessHandlerNotFoundException e) {
            logger.error("process handler is not defined for {} process", "METERPROCESSOR");
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
        instance.setId(new ObjectId(dto.getId()));
        instance.setProcessInstanceId(new ObjectId(dto.getProcessInstanceId()));
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


    protected abstract String entityType(Event<?, ?> event);

    public abstract String entityId(Event<?, ?> event);
}
