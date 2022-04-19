package com.aktimetrix.service.planner;

import com.aktimetrix.service.planner.model.MeasurementInstance;
import com.aktimetrix.service.planner.model.ProcessInstance;
import com.aktimetrix.service.planner.model.StepInstance;
import com.aktimetrix.service.planner.service.MeasurementInstanceService;
import com.aktimetrix.service.planner.service.ProcessInstanceService;
import com.aktimetrix.service.planner.service.StepInstanceService;
import com.aktimetrix.service.planner.transferobjects.Event;
import com.aktimetrix.service.planner.transferobjects.MeasurementInstanceDTO;
import com.aktimetrix.service.planner.transferobjects.ProcessInstanceDTO;
import com.aktimetrix.service.planner.transferobjects.StepInstanceDTO;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.time.LocalDateTime;
import java.util.function.Consumer;

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

    public static void main(String[] args) {
        SpringApplication.run(PlannerApplication.class, args);
    }

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
    }

    @Bean
    public Consumer<Event<ProcessInstanceDTO, Void>> processConsumer() {
        return payload -> {
            final ProcessInstanceDTO dto = payload.getEntity();
            logger.debug("process event received {}", dto);
            if (CREATED.equalsIgnoreCase(payload.getEventCode()) && PROCESS_EVENT.equalsIgnoreCase(payload.getEventType())) {
                logger.debug("saving the process created event.");
                processInstanceService.saveProcessInstance(getProcessInstance(dto));
            }
        };
    }

    @Bean
    public Consumer<Event<MeasurementInstanceDTO, Void>> measurementConsumer() {
        return payload -> {
            final MeasurementInstanceDTO dto = payload.getEntity();
            logger.debug("measurement event received {}", dto);
            if (CAPTURED.equalsIgnoreCase(payload.getEventCode()) && MEASUREMENT_EVENT.equalsIgnoreCase(payload.getEventType())) {
                logger.debug("saving the measurement created event.");
                measurementInstanceService.saveMeasurementInstance(getMeasurementInstance(dto));
            }
        };
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