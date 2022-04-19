package com.aktimetrix.service.processor.ciq.cdmpc.service;

import com.aktimetrix.service.processor.ciq.cdmpc.impl.CiQStepDefinitionProvider;
import com.aktimetrix.service.processor.core.api.MetadataProvider;
import com.aktimetrix.service.processor.core.api.ProcessContext;
import com.aktimetrix.service.processor.core.exception.DefinitionNotFoundException;
import com.aktimetrix.service.processor.core.impl.AbstractProcessor;
import com.aktimetrix.service.processor.core.model.ProcessInstance;
import com.aktimetrix.service.processor.core.model.StepInstance;
import com.aktimetrix.service.processor.core.process.ProcessType;
import com.aktimetrix.service.processor.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.service.processor.core.referencedata.model.StepDefinition;
import com.aktimetrix.service.processor.core.service.ProcessInstancePublisherService;
import com.aktimetrix.service.processor.core.service.ProcessInstanceService;
import com.aktimetrix.service.processor.core.service.StepInstancePublisherService;
import com.aktimetrix.service.processor.core.service.StepInstanceService;
import com.aktimetrix.service.processor.core.stereotypes.ProcessHandler;
import com.aktimetrix.service.processor.core.transferobjects.Cargo;
import com.aktimetrix.service.processor.core.transferobjects.Itinerary;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;


@Component
@ProcessHandler(processType = ProcessType.CDMP_C)
public class A2AProcessInstanceProcess extends AbstractProcessor {

    final private static Logger logger = LoggerFactory.getLogger(A2AProcessInstanceProcess.class);
    final private static String PROCESS_DEFINITION = "processDefinition";
    final private static String EVENT_DATA = "eventData";
    final private static String ENTITY = "entity";
    final private static String ENTITY_ID = "entityId";

    final private CDMPCCommonUtil cdmpcCommonUtil;
    final private StepInstanceService stepInstanceService;
    final private ProcessInstanceService processInstanceService;
    @Qualifier("stepMetadataProvider")
    final private MetadataProvider<Itinerary> stepMetadataProvider;
    @Qualifier("processMetadataProvider")
    final private MetadataProvider<Cargo> processMetadataProvider;

    public A2AProcessInstanceProcess(ProcessInstancePublisherService processInstancePublisherService,
                                     StepInstancePublisherService stepInstancePublisherService,
                                     CDMPCCommonUtil cdmpcCommonUtil,
                                     StepInstanceService stepInstanceService, ProcessInstanceService processInstanceService,
                                     MetadataProvider<Itinerary> stepMetadataProvider, MetadataProvider<Cargo> processMetadataProvider) {
        super(processInstancePublisherService, stepInstancePublisherService);
        this.cdmpcCommonUtil = cdmpcCommonUtil;
        this.stepInstanceService = stepInstanceService;
        this.processInstanceService = processInstanceService;
        this.stepMetadataProvider = stepMetadataProvider;
        this.processMetadataProvider = processMetadataProvider;
    }

    /**
     * Creates the process instance
     *
     * @param context context
     */
    @Transactional
    public void doProcess(ProcessContext context) {
        ProcessDefinition definition = (ProcessDefinition) context.getProperty(PROCESS_DEFINITION);
        Itinerary itinerary = (Itinerary) context.getProperty(EVENT_DATA);
        Cargo cargo = (Cargo) context.getProperty(ENTITY);
        String tenant = context.getTenant();
        String entityId = (String) context.getProperty(ENTITY_ID);

        ProcessInstance processInstance = getProcessInstance(definition, cargo, tenant, entityId);

        // get Step definitions
        final CiQStepDefinitionProvider stepDefinitionProvider = new CiQStepDefinitionProvider(definition.getSteps(), cargo, itinerary);
        try {
            final List<StepDefinition> stepDefinitions = stepDefinitionProvider.getDefinitions();
            logger.info("Saving Process Instance");
            final ProcessInstance savedProcessInstance = saveProcessInstance(processInstance);

            logger.debug("placing the process instance into context");
            context.setProcessInstance(savedProcessInstance);

            logger.info("Saving the step instances..");
            final Map<String, Object> stepMetadata = this.stepMetadataProvider.getMetadata(itinerary);

            final List<StepInstance> stepInstances = saveStepInstances(tenant, stepDefinitions, processInstance.getId(),
                    stepMetadata);

            logger.debug("placing the step instance into context");
            context.setStepInstances(stepInstances);

            StepInstance bkdStepInstance = saveBKDStepInstance(tenant, processInstance.getId(), cargo, stepMetadata);
            // assuming BKD is step for simplicity TODO
            logger.info("Capturing step planned measurements..");
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

    private ProcessInstance getProcessInstance(ProcessDefinition definition, Cargo cargo, String tenant, String entityId) {
        // check process instance already exists for this entity type, entity id, process code combination
        ProcessInstance processInstance = this.processInstanceService.getProcessInstance(tenant,
                definition.getProcessCode(), definition.getEntityType(), entityId);
        if (processInstance == null) {
            Map<String, Object> processMetadata = this.processMetadataProvider.getMetadata(cargo);
            processInstance = createProcessInstance(definition, entityId, processMetadata);
        }
        return processInstance;
    }

    /**
     * Creates the process instance object
     *
     * @param definition process definition
     * @param entityId   entity id
     * @param metadata   process metadata
     * @return process instance
     */
    private ProcessInstance createProcessInstance(ProcessDefinition definition, String entityId,
                                                  Map<String, Object> metadata) {
        ProcessInstance processInstance = new ProcessInstance(definition);
        processInstance.setMetadata(metadata);
        processInstance.setEntityId(entityId);
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


    /**
     * Create booking step instance
     *
     * @param processInstanceId process instance id
     * @param cargo             cargo
     * @param tenant            tenant
     * @return step instance
     */

    private StepInstance saveBKDStepInstance(String tenant, ObjectId processInstanceId, Cargo cargo, Map<String, Object> metadata) {
        StepInstance stepInstance = this.stepInstanceService
                .prepareStepInstanceObject(tenant, processInstanceId, "BKD", null, null, "" + 1, "Created");

        metadata.putAll(cdmpcCommonUtil.getProcessInstanceMetadata(cargo));
        logger.info(" metadata for the bkd is :: {}", metadata);

        stepInstance.setMetadata(metadata);
        stepInstance.setLocationCode(cargo.getOrigin().getCode());
        // save step instance
        return this.stepInstanceService.saveStepInstance(stepInstance);
//        Calculate  and save BKD measurements
//        cdmpcCommonUtil.calculateAndSaveMeasurements(cargo, itinerary);
    }

    /**
     * Saves the Process Instance
     *
     * @param processInstance process instances to be saved
     * @return saved process instance
     */
    private ProcessInstance saveProcessInstance(ProcessInstance processInstance) {
        // Save Process Instance
        if (processInstance.getId() == null) {
            processInstance = this.processInstanceService.saveProcessInstance(processInstance);
            logger.info("Process Instance Id in A2AProcessInstanceService:" + processInstance.getId());
        }
        return processInstance;
    }
}
