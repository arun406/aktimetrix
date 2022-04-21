package com.aktimetrix.service.planner;

import com.aktimetrix.service.planner.api.Planner;
import com.aktimetrix.service.planner.model.MeasurementInstance;
import com.aktimetrix.service.planner.model.Plan;
import com.aktimetrix.service.planner.model.ProcessInstance;
import com.aktimetrix.service.planner.model.StepInstance;
import com.aktimetrix.service.planner.service.MeasurementInstanceService;
import com.aktimetrix.service.planner.service.ProcessInstanceService;
import com.aktimetrix.service.planner.service.StepInstanceService;
import com.aktimetrix.service.planner.transferobjects.Event;
import com.aktimetrix.service.planner.transferobjects.MeasurementInstanceDTO;
import com.aktimetrix.service.planner.transferobjects.PlanInstanceDTO;
import com.aktimetrix.service.planner.transferobjects.ProcessInstanceDTO;
import com.aktimetrix.service.planner.transferobjects.StepInstanceDTO;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@ComponentScan(basePackages = {"com.aktimetrix.service.planner"})
@SpringBootApplication
public class PlannerApplication {

    final private static Logger logger = LoggerFactory.getLogger(PlannerApplication.class.getName());
    private static final String CREATED = "CREATED";
    private static final String CAPTURED = "CAPTURED";
    private static final String STEP_EVENT = "Step_Event";
    private static final String PROCESS_EVENT = "Process_Event";
    private static final String MEASUREMENT_EVENT = "Measurement_Event";

    @Autowired
    private StepInstanceService stepInstanceService;

    @Autowired
    private ProcessInstanceService processInstanceService;

    @Autowired
    private MeasurementInstanceService measurementInstanceService;

    @Autowired
    private Planner planner;

    public static void main(String[] args) {
        SpringApplication.run(PlannerApplication.class, args);
    }
/*
    @Bean
    public Consumer<Event<StepInstanceDTO, Void>> stepConsumer() {
        return payload -> {
            final StepInstanceDTO dto = payload.getEntity();
            logger.debug("step event received {}", dto);
            if (CREATED.equalsIgnoreCase(payload.getEventCode()) && STEP_EVENT.equalsIgnoreCase(payload.getEventType())) {
                logger.debug("saving the step created event.");
                stepInstanceService.saveStepInstance(getStepInstance(dto));
            }
        };
    }*/

    @Bean
    public Consumer<Event<ProcessInstanceDTO, Void>> processConsumer() {
        return payload -> {
            final ProcessInstanceDTO dto = payload.getEntity();
            final String tenantKey = payload.getTenantKey();
            logger.debug("process event received {}", dto);
            if (CREATED.equalsIgnoreCase(payload.getEventCode()) && PROCESS_EVENT.equalsIgnoreCase(payload.getEventType())) {
                logger.debug("saving the process created event.");
                final ProcessInstance processInstance = processInstanceService.saveProcessInstance(getProcessInstance(dto));
                logger.debug("process instance is saved successfully.");
                // create or update plan
                final Plan plan = planner.createPlan(tenantKey, processInstance);

                final boolean complete = isPlanComplete(plan, tenantKey);
                logger.debug("plan completion status {}", complete);
                if (complete) {
                    logger.debug("plan is complete");
                    // Get the step measurements.
                    final List<MeasurementInstance> processMeasurements = getMeasurementInstances(tenantKey, plan.getProcessInstanceId());
                    final PlanInstanceDTO planInstanceDTO = getPlanInstanceDTO(processInstance, plan, processMeasurements);
                    logger.debug("plan instance dto : {}", planInstanceDTO);
                    // publish plan created event when all required plan measurements are received.
                }
            }
        };
    }


    @Bean
    public Consumer<Event<MeasurementInstanceDTO, Void>> measurementConsumer() {
        return payload -> {
            ProcessInstance processInstance = null;
            final MeasurementInstanceDTO dto = payload.getEntity();
            logger.debug("measurement event received {}", dto);
            if (CAPTURED.equalsIgnoreCase(payload.getEventCode()) && MEASUREMENT_EVENT.equalsIgnoreCase(payload.getEventType())) {
                logger.debug("saving the measurement created event.");
                final MeasurementInstance measurementInstance = measurementInstanceService.saveMeasurementInstance(getMeasurementInstance(dto));
                logger.debug("measurement instance is saved successfully.");
                final String processInstanceId = dto.getProcessInstanceId();
                final String tenantKey = payload.getTenantKey();
                logger.debug("lookup for the existing plan for {}", processInstanceId);
                Plan plan = planner.getActivePlan(tenantKey, processInstanceId);
                if (plan == null) {
                    // creating a plan with process instance (only with id)
                    processInstance = new ProcessInstance();
                    processInstance.setId(measurementInstance.getProcessInstanceId());
                    plan = planner.createPlan(tenantKey, processInstance);
                }
                final boolean complete = isPlanComplete(plan, tenantKey);
                if (complete) {
                    // Get the step measurements.
                    final List<MeasurementInstance> processMeasurements = getMeasurementInstances(tenantKey, plan.getProcessInstanceId());
                    final PlanInstanceDTO planInstanceDTO = getPlanInstanceDTO(processInstance, plan, processMeasurements);
                    logger.debug("plan instance dto : {}", planInstanceDTO);
                    // publish plan created event when all required plan measurements are received.
                    Event<PlanInstanceDTO, Void> event = new Event<>();
                    event.setEventId(UUID.randomUUID().toString());
                    event.setEventType("Plan_Event");
                    event.setEventCode("CREATED");
                    event.setEventName("Plan Instance Created Event");
                    event.setEventTime(ZonedDateTime.now());
                    event.setEventUTCTime(LocalDateTime.now(ZoneOffset.UTC));
                    event.setEntityId(String.valueOf(planInstanceDTO.getId()));
                    event.setEntityType("com.aktimetrix.plan.instance");
                    event.setSource("Planner");
                    event.setTenantKey(planInstanceDTO.getTenant());
                    event.setEntity(planInstanceDTO);
                }
            }
        };
    }

    private boolean isPlanComplete(Plan plan, String tenantKey) {
        // get process instance and step instances.
        ObjectId processInstanceId = plan.getProcessInstanceId();
        final ProcessInstance processInstance = processInstanceService.getProcessInstance(tenantKey, processInstanceId);
        final List<StepInstance> steps = processInstance.getSteps();
        logger.debug("steps: {}", steps);
        if (steps == null || steps.isEmpty()) {
            return false;
        }
        List<MeasurementInstance> measurementInstances = getMeasurementInstances(tenantKey, processInstance.getId());
        final List<String> expectedSteps = steps.stream().map(StepInstance::getId).map(ObjectId::toString).collect(Collectors.toList());
        final boolean allMatch = measurementInstances.stream()
                .filter(mi -> mi.getType().equalsIgnoreCase("P"))
                .map(MeasurementInstance::getStepInstanceId)
                .map(ObjectId::toString)
                .collect(Collectors.toSet()).stream()
                .allMatch(expectedSteps::remove);

        if (allMatch && expectedSteps.isEmpty() && plan.getProcessInstanceId() != null && plan.getProcessCode() != null) {
            logger.debug("all step measurements are available. publishing the plan...");
            return true;
        }
        return false;
    }

    private List<MeasurementInstance> getMeasurementInstances(String tenantKey, ObjectId processInstanceId) {
        return measurementInstanceService.getProcessMeasurements(tenantKey, processInstanceId);
    }


    private PlanInstanceDTO getPlanInstanceDTO(ProcessInstance processInstance, Plan plan, List<MeasurementInstance> processMeasurements) {
        PlanInstanceDTO planInstanceDTO = new PlanInstanceDTO();
        planInstanceDTO.setProcessInstance(getProcessInstanceDTO(processInstance));
        planInstanceDTO.setActiveInd(plan.getActiveInd());
        planInstanceDTO.setCompleteInd(plan.getCompleteInd());
        planInstanceDTO.setCreatedOn(plan.getCreatedOn());
        planInstanceDTO.setStatus(plan.getStatus());
        planInstanceDTO.setId(plan.getId());
        planInstanceDTO.setTenant(plan.getTenant());
        planInstanceDTO.setVersion(plan.getVersion());
        if (processMeasurements != null && !processMeasurements.isEmpty()) {
            planInstanceDTO.setStepMeasurements(getStepMeasurementInstanceDTOs(processMeasurements));
        }
        return planInstanceDTO;
    }

    private ProcessInstanceDTO getProcessInstanceDTO(ProcessInstance processInstance) {
        return ProcessInstanceDTO.builder()
                .metadata(processInstance.getMetadata())
                .active(processInstance.isActive())
                .categoryCode(processInstance.getCategoryCode())
                .complete(processInstance.isComplete())
                .entityId(processInstance.getEntityId())
                .processCode(processInstance.getProcessCode())
                .entityType(processInstance.getEntityType())
                .id(processInstance.getId().toString())
                .status(processInstance.getStatus())
                .subCategoryCode(processInstance.getSubCategoryCode())
                .tenant(processInstance.getTenant())
                .valid(processInstance.isValid())
                .version(processInstance.getVersion())
                .processCode(processInstance.getProcessCode())
                .steps(getStepInstanceDTOs(processInstance.getSteps()))
                .build();
    }

    private List<StepInstanceDTO> getStepInstanceDTOs(List<StepInstance> steps) {
        return steps.stream().map(this::getStepInstanceDTO).collect(Collectors.toList());
    }

    private StepInstanceDTO getStepInstanceDTO(StepInstance stepInstance) {
        return StepInstanceDTO.builder()
                .id(stepInstance.getId().toString())
                .status(stepInstance.getStatus())
                .createdOn(stepInstance.getCreatedOn())
                .functionalCtx(stepInstance.getFunctionalCtx())
                .groupCode(stepInstance.getGroupCode())
                .locationCode(stepInstance.getLocationCode())
                .metadata(stepInstance.getMetadata())
                .stepCode(stepInstance.getStepCode())
                .tenant(stepInstance.getTenant())
                .version(stepInstance.getVersion())
                .build();
    }

    private Multimap<String, MeasurementInstanceDTO> getStepMeasurementInstanceDTOs(List<MeasurementInstance> processMeasurements) {
        Multimap<String, MeasurementInstanceDTO> stepMeasurementInstanceDTOs = ArrayListMultimap.create();

        for (MeasurementInstance processMeasurement : processMeasurements) {
            stepMeasurementInstanceDTOs.put(processMeasurement.getStepCode(), getStepMeasurementInstanceDTO(processMeasurement));
        }
        return stepMeasurementInstanceDTOs;
    }

    private MeasurementInstanceDTO getStepMeasurementInstanceDTO(MeasurementInstance m) {
        return MeasurementInstanceDTO.builder()
                .code(m.getCode())
                .id(m.getId().toString())
                .measuredAt(m.getMeasuredAt())
                .createdOn(m.getCreatedOn())
                .tenant(m.getTenant())
                .type(m.getType())
                .unit(m.getUnit())
                .value(m.getValue())
                .build();
    }

    private MeasurementInstance getMeasurementInstance(MeasurementInstanceDTO dto) {
        MeasurementInstance instance = new MeasurementInstance();
        instance.setStepInstanceId(new ObjectId(dto.getStepInstanceId()));
        instance.setProcessInstanceId(new ObjectId(dto.getProcessInstanceId()));
        instance.setType(dto.getType());
        instance.setUnit(dto.getUnit());
        instance.setCode(dto.getCode());
        instance.setValue(dto.getValue());
        instance.setId(new ObjectId(dto.getId()));
        instance.setCreatedOn(dto.getCreatedOn());
        instance.setStepCode(dto.getStepCode());
        instance.setMeasuredAt(dto.getMeasuredAt());
        instance.setTenant(dto.getTenant());
        return instance;
    }


    private ProcessInstance getProcessInstance(ProcessInstanceDTO dto) {
        ProcessInstance instance = new ProcessInstance();
        instance.setId(new ObjectId(dto.getId()));
        instance.setEntityId(dto.getEntityId());
        instance.setEntityType(dto.getEntityType());
        instance.setCreatedOn(LocalDateTime.now());
        instance.setMetadata(dto.getMetadata());
        instance.setStatus(dto.getStatus());
        instance.setTenant(dto.getTenant());
        instance.setVersion(dto.getVersion());
        instance.setActive(dto.isActive());
        instance.setComplete(dto.isComplete());
        instance.setProcessCode(dto.getProcessCode());
        instance.setCategoryCode(dto.getCategoryCode());
        instance.setSubCategoryCode(dto.getSubCategoryCode());
        return instance;
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
}