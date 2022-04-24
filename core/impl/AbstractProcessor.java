package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.api.PreProcessor;
import com.aktimetrix.core.api.ProcessContext;
import com.aktimetrix.core.api.ProcessType;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.api.Registry;
import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.service.ProcessInstancePublisherService;
import com.aktimetrix.core.service.ProcessInstanceService;
import com.aktimetrix.core.service.StepInstancePublisherService;
import com.aktimetrix.core.service.StepInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@Slf4j
public abstract class AbstractProcessor implements Processor {

    final private static Logger logger = LoggerFactory.getLogger(AbstractProcessor.class);

    @Autowired
    private StepInstanceService stepInstanceService;
    @Autowired
    private ProcessInstanceService processInstanceService;

    // default post processors
    @Autowired
    private ProcessInstancePublisherService processInstancePublisherService;
    @Autowired
    private StepInstancePublisherService stepInstancePublisherService;

    @Autowired
    private Registry registry;

    /**
     * @param context process context
     */
    @Override
    public void process(ProcessContext context) {
        // call pre processors
        executePreProcessors(context);
        // call do process
        doProcess(context);
        // post processors
        executePostProcessors(context);
    }

    private void executePreProcessors(ProcessContext context) {
        // get the preprocessors from registry
        log.debug("executing the preprocessors");

        final List<PreProcessor> preProcessors = getPreProcessor(ProcessType.A2ATRANSPORT); // TODO remove the hard coding
        preProcessors.forEach(preProcessor -> preProcessor.process(context));
    }


    @Transactional
    public void doProcess(ProcessContext context) {
        ProcessInstance processInstance = getProcessInstance(context);
        try {
            // get Step definitions
            final List<StepDefinition> stepDefinitions = getStepDefinitions(context);
            logger.info("Saving Process Instance");
            final ProcessInstance savedProcessInstance = saveProcessInstance(processInstance);
            logger.debug("placing the process instance into context");
//            context.setProcessInstance(savedProcessInstance);
            logger.info("Saving the step instances..");
            final List<StepInstance> stepInstances = saveStepInstances(context.getTenant(), stepDefinitions, processInstance.getId(),
                    getStepMetadata(context));
            logger.debug("placing the step instance into context");
            processInstance.setSteps(Objects.requireNonNullElseGet(stepInstances, ArrayList::new));

            context.setStepInstances(stepInstances);
            context.setProcessInstance(processInstance);

        } catch (DefinitionNotFoundException e) {
            logger.error("step definitions are not available for this process", e);
        }
    }

    public List<StepDefinition> getStepDefinitions(ProcessContext context) throws DefinitionNotFoundException {
        return new DefaultStepDefinitionProvider((ProcessDefinition) context.getProperty(Constants.PROCESS_DEFINITION)).getDefinitions();
    }

    protected abstract Map<String, Object> getStepMetadata(ProcessContext context);

    protected abstract Map<String, Object> getProcessMetadata(ProcessContext context);

    /**
     * Execute the post processor
     *
     * @param context process context
     */
    private void executePostProcessors(ProcessContext context) {
        // TODO  get the post processors from registry
        log.debug("executing post processors");
        // publish the process instance event
//        this.processInstancePublisherService.process(context);
//        this.stepInstancePublisherService.process(context);
        final List<PostProcessor> postProcessors = getPostProcessor(ProcessType.A2ATRANSPORT); // TODO remove the hard coding
        postProcessors.forEach(postProcessor -> postProcessor.process(context));
    }


    /**
     * Saves the Process Instance
     *
     * @param processInstance process instances to be saved
     * @return saved process instance
     */
    private ProcessInstance saveProcessInstance(ProcessInstance processInstance) {
        // Save Process Instance only if it's already not exists
        if (processInstance.getId() == null) {
            return this.processInstanceService.saveProcessInstance(processInstance);
        }
        return processInstance;
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
    private List<StepInstance> saveStepInstances(String tenant, List<StepDefinition> stepDefinitions,
                                                 ObjectId processInstanceId, Map<String, Object> metadata) {
        return this.stepInstanceService
                .save(tenant, stepDefinitions, metadata, processInstanceId);
    }

    private ProcessInstance getProcessInstance(ProcessContext context) {
        ProcessDefinition definition = (ProcessDefinition) context.getProperty(Constants.PROCESS_DEFINITION);
        String tenant = context.getTenant();
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

    private List<PostProcessor> getPostProcessor(ProcessType processType) {
        Predicate<RegistryEntry> predicate1 = re -> re.hasAttribute(Constants.ATT_POST_PROCESSOR_SERVICE)
                && re.attribute(Constants.ATT_POST_PROCESSOR_SERVICE).equals(Constants.VAL_YES);
        Predicate<RegistryEntry> predicate2 = re -> (re.attribute(Constants.ATT_POST_PROCESSOR_PROCESS_TYPE) == processType);

        final List<Object> postProcessors = this.registry.lookupAll(predicate1.and(predicate2));
        logger.debug("Applicable post processors {}", postProcessors);

        if (postProcessors != null && !postProcessors.isEmpty()) {
            return postProcessors.stream().map(m -> (PostProcessor) m).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<PreProcessor> getPreProcessor(ProcessType processType) {
        Predicate<RegistryEntry> predicate1 = re -> re.hasAttribute(Constants.ATT_PRE_PROCESSOR_SERVICE)
                && re.attribute(Constants.ATT_PRE_PROCESSOR_SERVICE).equals(Constants.VAL_YES);
        Predicate<RegistryEntry> predicate2 = re -> (re.attribute(Constants.ATT_PRE_PROCESSOR_PROCESS_TYPE) == processType);

        final List<Object> preProcessors = this.registry.lookupAll(predicate1.and(predicate2));
        logger.debug("Applicable pre processors {}", preProcessors);

        if (preProcessors != null && !preProcessors.isEmpty()) {
            return preProcessors.stream().map(m -> (PreProcessor) m).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
