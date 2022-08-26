package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PreProcessor;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.service.ProcessInstanceService;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.service.StepInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public abstract class AbstractProcessor implements Processor {

    @Autowired
    protected StepInstanceService stepInstanceService;
    @Autowired
    protected ProcessInstanceService processInstanceService;
    @Autowired
    public RegistryService registryService;

    /**
     * process the request
     *
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

    /**
     * execute the pre processors
     *
     * @param context
     */
    protected void executePreProcessors(Context context) {
        // get the preprocessors from registry
        log.debug("executing the preprocessors");
        // get default preprocessor
        final List<PreProcessor> defaultPreProcessors = registryService.getPreProcessor(Constants.DEFAULT_PROCESS_TYPE, Constants.DEFAULT_PROCESS_CODE);
        defaultPreProcessors.forEach(preProcessor -> preProcessor.preProcess(context));
    }


    /**
     * do process
     *
     * @param context
     * @throws DefinitionNotFoundException
     */
    @Transactional
    public void doProcess(Context context) throws DefinitionNotFoundException {
        ProcessInstance processInstance = createProcessInstance(context);
        log.info("Saving Process Instance");
        this.saveProcessInstance(processInstance);
        context.setProcessInstance(processInstance);
        log.debug("process instance id: {}", processInstance.getId());
        final List<StepInstance> stepInstances = createStepInstances(context);
        if (stepInstances == null) {
            processInstance.getSteps().addAll(new ArrayList<>());
            log.debug("placing the process and step instance(s) into context");
            context.setStepInstances(stepInstances);
            return;
        }

        log.debug("step instances size :{}", stepInstances.size());
        final List<StepInstance> finalList = stepInstances.stream().map(stepInstance -> {
            stepInstance.setProcessInstanceId(processInstance.getId());
            return stepInstance;
        }).collect(Collectors.toList());
        this.stepInstanceService.save(finalList);
        log.debug("placing the step instance into context");
        processInstance.setSteps(stepInstances);
        saveProcessInstance(processInstance);// update
        log.debug("placing the process and step instance(s) into context");
        context.setStepInstances(stepInstances);
    }

    /**
     * creating the step instances
     *
     * @param context
     * @return
     * @throws DefinitionNotFoundException
     */
    protected List<StepInstance> createStepInstances(Context context) throws DefinitionNotFoundException {
        List<StepDefinition> stepDefinitions = getStepDefinitions(context);
        log.info("creating the step instances..");
        final Map<String, Object> stepMetadata = getStepMetadata(context);
        return createStepInstances(context.getTenant(), stepDefinitions, stepMetadata);
    }

    /**
     * returns the step definitions
     *
     * @param context
     * @return
     * @throws DefinitionNotFoundException
     */
    public List<StepDefinition> getStepDefinitions(Context context) throws DefinitionNotFoundException {
        final ProcessDefinition processDefinition = (ProcessDefinition) context.getProperty(Constants.PROCESS_DEFINITION);
        return processDefinition.getSteps();
    }

    protected abstract Map<String, Object> getStepMetadata(Context context);

    protected abstract Map<String, Object> getProcessMetadata(Context context);

    /**
     * Execute the post processor
     *
     * @param context process context
     */
    public void executePostProcessors(Context context) {
        log.debug("executing post processors");
    }

    /**
     * Saves the Process Instance
     *
     * @param processInstance process instances to be saved
     * @return saved process instance
     */
    protected ProcessInstance saveProcessInstance(ProcessInstance processInstance) {
        // Save Process Instance only if it's already not exists
        if (processInstance.getId() != null) {
            processInstance.setModifiedOn(LocalDateTime.now());
        }
        return this.processInstanceService.saveProcessInstance(processInstance);
    }

    /**
     * Save Step Instances
     *
     * @param tenant          tenant
     * @param stepDefinitions step definitions
     * @param metadata        metadata
     * @return step instance collection
     */
    @Transactional
    public List<StepInstance> createStepInstances(String tenant, List<StepDefinition> stepDefinitions, Map<String, Object> metadata) {
        return this.stepInstanceService.save(tenant, stepDefinitions, metadata);
    }

    /**
     * create or update process instance
     *
     * @param context
     * @return
     */
    protected ProcessInstance createProcessInstance(Context context) {
        ProcessDefinition definition = (ProcessDefinition) context.getProperty(Constants.PROCESS_DEFINITION);
        log.debug("definition code: {}", definition.getProcessCode());
        String entityId = (String) context.getProperty(Constants.ENTITY_ID);
        String entityType = (String) context.getProperty(Constants.ENTITY_TYPE);
        log.debug("entity id: {}, entity type :{}", entityId, entityType);
        return createProcessInstance(context, definition, entityType, entityId);
    }


    /**
     * create new process instance
     *
     * @param context
     * @param definition
     * @param entityId
     * @return
     */
    protected ProcessInstance createProcessInstance(Context context, ProcessDefinition definition, String
            entityType, String entityId) {
        ProcessInstance processInstance = new ProcessInstance(definition);
        processInstance.setTenant(context.getTenant());
        processInstance.setEntityId(entityId);
        processInstance.setEntityType(entityType);
        processInstance.setSteps(new ArrayList<>());
        processInstance.setCreatedOn(LocalDateTime.now(ZoneOffset.UTC));
        processInstance.setMetadata(this.getProcessMetadata(context));
        return processInstance;
    }
}
