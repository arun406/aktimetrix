package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.*;
import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public abstract class AbstractProcessor implements Processor {

    @Autowired
    private StepInstanceService stepInstanceService;
    @Autowired
    private ProcessInstanceService processInstanceService;
    @Autowired
    private RegistryService registryService;

    /**
     * @param context process context
     */
    @Override
    public void process(Context context) {
        // call pre processors
        executePreProcessors(context);
        // call do process
        try {
            doProcess(context);
        } catch (DefinitionNotFoundException e) {
            log.error(e.getLocalizedMessage(), e);
            // set errors in the context
        }
        // post processors
        executePostProcessors(context);
    }

    protected void executePreProcessors(Context context) {
        // get the preprocessors from registry
        log.debug("executing the preprocessors");
        // get default preprocessor
        final List<PreProcessor> defaultPreProcessors = registryService.getPreProcessor(Constants.DEFAULT_PROCESS_TYPE, Constants.DEFAULT_PROCESS_CODE);
        defaultPreProcessors.forEach(preProcessor -> preProcessor.preProcess(context));
    }

    @Transactional
    public void doProcess(Context context) throws DefinitionNotFoundException {
        ProcessInstance processInstance = getProcessInstance(context);

        List<StepDefinition> stepDefinitions = getStepDefinitions(context);
        log.info("Saving Process Instance");
        final ProcessInstance savedProcessInstance = saveProcessInstance(processInstance);
        log.debug("placing the process instance into context");
        log.info("Saving the step instances..");
        final List<StepInstance> stepInstances = saveStepInstances(context.getTenant(), stepDefinitions, processInstance.getId(),
                getStepMetadata(context));
        this.stepInstanceService.save(stepInstances);
        log.debug("placing the step instance into context");
        processInstance.getSteps().addAll(Objects.requireNonNullElseGet(stepInstances, ArrayList::new));
        context.setStepInstances(stepInstances);
        context.setProcessInstance(processInstance);
    }

    public List<StepDefinition> getStepDefinitions(Context context) throws DefinitionNotFoundException {
        return new DefaultStepDefinitionProvider((ProcessDefinition) context.getProperty(Constants.PROCESS_DEFINITION)).getDefinitions();
    }

    protected abstract Map<String, Object> getStepMetadata(Context context);

    protected abstract Map<String, Object> getProcessMetadata(Context context);

    /**
     * Execute the post processor
     *
     * @param context process context
     */
    private void executePostProcessors(Context context) {
        log.debug("executing post processors");
        // publish the process instance event
        final List<PostProcessor> postProcessors = registryService.getPostProcessor(context.getProcessType(), context.getProcessCode());
        if (!postProcessors.isEmpty()) {
            // order the post processor execution by priority TODO
            postProcessors.forEach(postProcessor -> postProcessor.process(context));
        }
        // default post processors
        final List<PostProcessor> defaultPostProcessors = registryService.getPostProcessor(Constants.DEFAULT_PROCESS_TYPE, Constants.DEFAULT_PROCESS_CODE);
        if (!defaultPostProcessors.isEmpty()) {
            defaultPostProcessors.forEach(postProcessor -> postProcessor.process(context));
        }
    }

    /**
     * Saves the Process Instance
     *
     * @param processInstance process instances to be saved
     * @return saved process instance
     */
    private ProcessInstance saveProcessInstance(ProcessInstance processInstance) {
        // Save Process Instance only if it's already not exists
        if (processInstance.getId() != null) {
            processInstance.setModifiedOn(LocalDateTime.now());
        }
        return this.processInstanceService.saveProcessInstance(processInstance);
    }

    /**
     * Save Step Instances
     *
     * @param tenant            tenant
     * @param stepDefinitions   step definitions
     * @param processInstanceId process instance id
     * @param metadata          metadata
     * @return step instance collection
     */
    @Transactional
    public List<StepInstance> saveStepInstances(String tenant, List<StepDefinition> stepDefinitions,
                                                String processInstanceId, Map<String, Object> metadata) {
        return this.stepInstanceService.save(tenant, stepDefinitions, metadata, processInstanceId);
    }

    /**
     * create or update process instance
     *
     * @param context
     * @return
     */
    protected ProcessInstance getProcessInstance(Context context) {
        ProcessDefinition definition = (ProcessDefinition) context.getProperty(Constants.PROCESS_DEFINITION);
        String entityId = (String) context.getProperty(Constants.ENTITY_ID);
        String entityType = (String) context.getProperty(Constants.ENTITY_TYPE);

        log.debug("looking for process instance with same entity id and entity type.");
        // check process instance already exists for this entity type, entity id, process code combination
        Page<ProcessInstance> page = processInstanceService.getProcessInstance(context.getTenant(),
                definition.getProcessType(), definition.getProcessCode(), definition.getEntityType(), entityId);
        ProcessInstance processInstance = null;
        Map<String, Object> processMetadata = this.getProcessMetadata(context);

        if (!page.isEmpty() && page.hasContent()) {
            processInstance = page.getContent().get(0);
            // compare quantity in both process instances
            Map<String, Object> metadata = processInstance.getMetadata();
            if (processInstance != null) {
                if (!processInstance.isComplete()) {
                    log.debug("retrieving existing incomplete process instances.");
                    List<StepInstance> stepInstances = stepInstanceService.getStepInstancesByProcessInstanceId(context.getTenant(), processInstance.getId());
                    processInstance.setSteps(stepInstances);
                    context.setStepInstances(stepInstances);
                    context.setProcessInstance(processInstance);
                    processInstance.setMetadata(processMetadata);
                    return processInstance;
                } else {
                    log.debug("compare the itineraries and verify only flight /date is changed.? and update the itinerary.");
                }
            }
        }
        log.debug("creating new process instance from the scratch.");
        processInstance = getProcessInstance(context, definition, entityId);
        processInstance.setMetadata(processMetadata);
        return processInstance;
    }


    /**
     * create new process instance
     *
     * @param context
     * @param definition
     * @param entityId
     * @return
     */
    private ProcessInstance getProcessInstance(Context context, ProcessDefinition definition, String entityId) {
        ProcessInstance processInstance = new ProcessInstance(definition);
        processInstance.setTenant(context.getTenant());
        processInstance.setEntityId(entityId);
        processInstance.setSteps(new ArrayList<>());
        processInstance.setCreatedOn(LocalDateTime.now());
        return processInstance;
    }
}
