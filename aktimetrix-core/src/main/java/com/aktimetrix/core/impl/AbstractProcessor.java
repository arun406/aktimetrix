package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.api.PreProcessor;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.exception.ProcessHandlerNotFoundException;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.service.ProcessInstanceService;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.service.StepInstanceService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for process handlers. Creates the process instance and its step instances for a business entity,
 * computes each step's planned measurements with the registered meters, and runs the pre- and post-processors of
 * the process type.
 * <p>
 * Subclasses decide which metadata to keep on the process instance and on its step instances.
 *
 * @author arun kumar kandakatla
 */
public abstract class AbstractProcessor implements Processor {
    final private static Logger logger = LoggerFactory.getLogger(AbstractProcessor.class);

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
        executePreProcessors(context);
        doProcess(context);
        planSteps(context);
        executePostProcessors(context);
    }

    private void executePreProcessors(Context context) {
        logger.debug("executing the preprocessors");
        final List<PreProcessor> preProcessors = registryService.getPreProcessor(context.getProcessType());
        preProcessors.forEach(preProcessor -> preProcessor.process(context));
    }

    public void doProcess(Context context) {
        ProcessInstance processInstance = getProcessInstance(context);
        if (processInstance.getId() != null) {
            // replayed start event: keep the existing steps and publish nothing new for them
            logger.info("Process instance {} already exists", processInstance.getId());
            processInstance.setSteps(stepInstanceService.getStepInstancesByProcessInstanceId(context.getTenant(),
                    processInstance.getId()));
            context.setStepInstances(new ArrayList<>());
            context.setProcessInstance(processInstance);
            return;
        }
        try {
            final List<StepDefinition> stepDefinitions = getStepDefinitions(context);
            logger.info("Saving Process Instance");
            saveProcessInstance(processInstance);
            logger.info("Saving the step instances..");
            final List<StepInstance> stepInstances = saveStepInstances(context.getTenant(), stepDefinitions,
                    processInstance.getId(), getStepMetadata(context));
            processInstance.setSteps(Objects.requireNonNullElseGet(stepInstances, ArrayList::new));

            context.setStepInstances(stepInstances);
            context.setProcessInstance(processInstance);
        } catch (DefinitionNotFoundException e) {
            logger.error("step definitions are not available for this process", e);
        }
    }

    /**
     * Computes the planned measurements of the newly created steps with the registered meters.
     */
    private void planSteps(Context context) {
        if (context.getStepInstances().isEmpty()) {
            return;
        }
        final Processor meterProcessor;
        try {
            meterProcessor = registryService.getProcessHandler(Constants.METER_PROCESSOR);
        } catch (ProcessHandlerNotFoundException e) {
            logger.warn("No meter processor registered; planned measurements are not computed");
            return;
        }
        for (StepInstance step : context.getStepInstances()) {
            DefaultContext stepContext = new DefaultContext();
            stepContext.setTenant(context.getTenant());
            stepContext.setProcessType(Constants.METER_PROCESSOR);
            stepContext.setStepInstances(new ArrayList<>(List.of(step)));
            meterProcessor.process(stepContext);
        }
    }

    public List<StepDefinition> getStepDefinitions(Context context) throws DefinitionNotFoundException {
        return new DefaultStepDefinitionProvider((ProcessDefinition) context.getProperty(Constants.PROCESS_DEFINITION)).getDefinitions();
    }

    /**
     * Metadata to store on each step instance, available to meters as {@code step.getMetadata()}.
     */
    protected abstract Map<String, Object> getStepMetadata(Context context);

    /**
     * Metadata to store on the process instance.
     */
    protected abstract Map<String, Object> getProcessMetadata(Context context);

    private void executePostProcessors(Context context) {
        logger.debug("executing post processors");
        final List<PostProcessor> postProcessors = registryService.getPostProcessor(context.getProcessType());
        postProcessors.forEach(postProcessor -> postProcessor.process(context));
    }

    private ProcessInstance saveProcessInstance(ProcessInstance processInstance) {
        return this.processInstanceService.saveProcessInstance(processInstance);
    }

    private List<StepInstance> saveStepInstances(String tenant, List<StepDefinition> stepDefinitions,
                                                 ObjectId processInstanceId, Map<String, Object> metadata) {
        return this.stepInstanceService
                .save(tenant, stepDefinitions, metadata, processInstanceId);
    }

    private ProcessInstance getProcessInstance(Context context) {
        ProcessDefinition definition = (ProcessDefinition) context.getProperty(Constants.PROCESS_DEFINITION);
        String entityId = (String) context.getProperty(Constants.ENTITY_ID);
        // check process instance already exists for this entity type, entity id, process code combination
        ProcessInstance processInstance = processInstanceService.getProcessInstance(context.getTenant(),
                definition.getProcessCode(), definition.getEntityType(), entityId);
        if (processInstance == null) {
            processInstance = new ProcessInstance(definition);
            processInstance.setMetadata(getProcessMetadata(context));
            processInstance.setEntityId(entityId);
        }
        return processInstance;
    }
}
