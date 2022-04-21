package com.aktimetrix.service.processor.core.impl;

import com.aktimetrix.service.processor.core.Constants;
import com.aktimetrix.service.processor.core.api.ProcessContext;
import com.aktimetrix.service.processor.core.api.Processor;
import com.aktimetrix.service.processor.core.exception.DefinitionNotFoundException;
import com.aktimetrix.service.processor.core.model.ProcessInstance;
import com.aktimetrix.service.processor.core.model.StepInstance;
import com.aktimetrix.service.processor.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.service.processor.core.referencedata.model.StepDefinition;
import com.aktimetrix.service.processor.core.service.ProcessInstancePublisherService;
import com.aktimetrix.service.processor.core.service.ProcessInstanceService;
import com.aktimetrix.service.processor.core.service.StepInstancePublisherService;
import com.aktimetrix.service.processor.core.service.StepInstanceService;
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

@Component
@Slf4j
public abstract class AbstractProcessor implements Processor {

    final private static Logger logger = LoggerFactory.getLogger(AbstractProcessor.class);

    @Autowired
    private StepInstanceService stepInstanceService;
    @Autowired
    private ProcessInstanceService processInstanceService;
    @Autowired
    private ProcessInstancePublisherService processInstancePublisherService;
    @Autowired
    private StepInstancePublisherService stepInstancePublisherService;


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
        // create and save step's plan measurements based on the step definition 'planMeasurements'
        /* this.processInstanceService.createStepMeasurements(tenant, bkdStepInstance, stepDefinitionMap, stepInstances);
       try {
            // Create a Plan if not exists
            Plan plan = plannerService.getPlan(tenant, processInstance.getId());
            if (plan == null) {
                plannerService.createPlan(tenant, processInstance, stepInstances);
            } else {
                plannerService.updatePlan(tenant, plan, processInstance, stepInstances);
            }
        } catch (UnableToCreatePlan unableToCreatePlan) {
            logger.error(unableToCreatePlan.getMessage());
        } catch (UnableToModifyPlan unableToModifyPlan) {
            logger.error(unableToModifyPlan.getMessage());
        }*/
        // set the process instance into the process context
//        ((DefaultProcessContext) context).setProcessInstance(processInstance);
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
        this.processInstancePublisherService.process(context);
        this.stepInstancePublisherService.process(context);
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
                .createStepInstances(tenant, stepDefinitions, metadata, processInstanceId);
    }

    public ProcessInstance getProcessInstance(ProcessContext context) {
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
}
