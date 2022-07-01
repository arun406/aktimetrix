package com.aktimetrix.core.event.handler;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.exception.MultipleProcessHandlersFoundException;
import com.aktimetrix.core.exception.ProcessHandlerNotFoundException;
import com.aktimetrix.core.impl.DefaultContext;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.ProcessInstanceDTO;
import com.aktimetrix.core.transferobjects.StepInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractProcessEventHandler implements EventHandler {

    final private RegistryService registryService;

    /**
     * @param event
     */
    @Override
    public void handle(Event<?, ?> event) {
        String processType = null;
        String processCode = null;
        log.info("Entity Id : {}", event.getEntityId());
        try {
            if (Constants.PROCESS_EVENT.equals(event.getEventType())) {
                processType = Constants.PROCESS_INSTANCE_TYPE;
            }
            if (Constants.PROCESS_CREATED.equals(event.getEventCode())) {
                processCode = Constants.PROCESS_INSTANCE_CREATE;
            } else if (Constants.PROCESS_UPDATED.equals(event.getEventCode())) {
                processCode = Constants.PROCESS_INSTANCE_UPDATE;
            } else if (Constants.PROCESS_CANCELLED.equals(event.getEventCode())) {
                processCode = Constants.PROCESS_INSTANCE_CANCEL;
            }
            log.debug("looking for process handler for {}-{}", processType, processCode);
            Processor processor = registryService.getProcessHandler(processType, processCode);
            DefaultContext context = prepareContext(event);
            processor.process(context);
        } catch (ProcessHandlerNotFoundException | MultipleProcessHandlersFoundException e) {
            log.error("error while getting the process handlers {}", e.getMessage());
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
        final ProcessInstanceDTO entity = (ProcessInstanceDTO) event.getEntity();
        processContext.setProperty("entity", entity);
        processContext.setProperty("eventData", event.getEventDetails());
        processContext.setTenant(event.getTenantKey());
        if (entity != null) {
            processContext.setProcessInstance(getProcessInstance(entity));
        }
        return processContext;
    }

    /**
     * Creates the Entity
     *
     * @param dto
     * @return
     */
    private ProcessInstance getProcessInstance(ProcessInstanceDTO dto) {
        ProcessInstance instance = new ProcessInstance();
        instance.setId(dto.getId());
        instance.setEntityId(dto.getEntityId());
        instance.setEntityType(dto.getEntityType());
        instance.setMetadata(dto.getMetadata());
        instance.setStatus(dto.getStatus());
        instance.setTenant(dto.getTenant());
        instance.setVersion(dto.getVersion());
        instance.setActive(dto.isActive());
        instance.setComplete(dto.isComplete());
        instance.setProcessCode(dto.getProcessCode());
        instance.setCategoryCode(dto.getCategoryCode());
        instance.setSubCategoryCode(dto.getSubCategoryCode());
        if (dto.getSteps() != null && !dto.getSteps().isEmpty())
            instance.setSteps(getStepInstances(dto.getSteps()));

        return instance;
    }

    /**
     * Creates the Step Instance Entities
     *
     * @param steps
     * @return
     */
    private List<StepInstance> getStepInstances(List<StepInstanceDTO> steps) {
        return steps.stream().map(this::getStepInstance).collect(Collectors.toList());
    }

    /**
     * Create single Step Instance
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
        return event.getEntityType();

    }

    public String entityId(Event<?, ?> event) {
        return event.getEntityId();
    }
}
