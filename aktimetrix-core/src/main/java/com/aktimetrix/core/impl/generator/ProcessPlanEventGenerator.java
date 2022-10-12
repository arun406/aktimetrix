package com.aktimetrix.core.impl.generator;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.EventGenerator;
import com.aktimetrix.core.model.*;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.Measurement;
import com.aktimetrix.core.transferobjects.ProcessInstanceDTO;
import com.aktimetrix.core.transferobjects.StepInstanceDTO;
import com.aktimetrix.core.transferobjects.ProcessPlanDTO;
import com.aktimetrix.core.transferobjects.StepPlanDTO;
import com.google.common.collect.ArrayListMultimap;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component("ProcessPlanEventGenerator")
public class ProcessPlanEventGenerator implements EventGenerator<ProcessPlanDTO, Void> {

    @Override
    public Event<ProcessPlanDTO, Void> generate(Object... object) {
        ProcessPlanInstance processPlanInstance = (ProcessPlanInstance) object[0];
        ProcessInstance processInstance = (ProcessInstance) object[1];
        return getProcessPlanInstanceEvent(processPlanInstance, processInstance);
    }

    /**
     * prepares the Event Object
     *
     * @param instance
     * @param processInstance
     * @return
     */
    private Event<ProcessPlanDTO, Void> getProcessPlanInstanceEvent(ProcessPlanInstance instance, ProcessInstance processInstance) {
        Event<ProcessPlanDTO, Void> event = new Event<>();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(Constants.PLAN_EVENT);
        if (instance.getStatus().equals(Constants.PLAN_CANCELLED)) {
            event.setEventCode(Constants.PLAN_CANCELLED);
            event.setEventName("Process Plan Instance Cancelled Event");
        } else {
            event.setEventCode(instance.getModifiedOn() == null ? Constants.PLAN_CREATED : Constants.PLAN_UPDATED);
            event.setEventName(instance.getModifiedOn() == null ? "Process Plan Instance Created Event" : "Process Plan Instance Updated Event");
        }
        event.setEventTime(ZonedDateTime.now());
        event.setEventUTCTime(LocalDateTime.now(ZoneOffset.UTC));
        event.setEntityId(String.valueOf(instance.getId()));
        event.setEntityType("com.aktimetrix.plan.instance");
        event.setSource("Planner");
        event.setVersion(String.valueOf(instance.getVersion()));
        event.setTenantKey(instance.getTenant());
        ProcessInstanceDTO processInstanceDTO = getProcessInstanceDTO(processInstance);
        event.setEntity(getProcessPlanDTO(instance, processInstanceDTO));
        return event;
    }

    private ProcessPlanDTO getProcessPlanDTO(ProcessPlanInstance instance, ProcessInstanceDTO processInstanceDTO) {
        return ProcessPlanDTO.builder()
                .activeInd(instance.getActiveInd())
                .entityId(instance.getEntityId())
                .entityType(instance.getEntityType())
                .approvedIndicator(instance.getApprovedIndicator())
                .completeInd(instance.getCompleteInd())
                .createdOn(instance.getCreatedOn())
                .processCode(instance.getProcessCode())
                .processType(instance.getProcessType())
                .directTruckingIndicator(instance.getDirectTruckingIndicator())
                .flightSpecificIndicator(instance.getFlightSpecificIndicator())
                .planNumber(instance.getPlanNumber())
                .status(instance.getStatus())
                .phaseNumber(instance.getPhaseNumber())
                .tenant(instance.getTenant())
                .id(instance.getId())
                .updateFlag(instance.getUpdateFlag())
                .version(instance.getVersion() + "")
                .shipmentIndicator(instance.getShipmentIndicator())
                .processInstance(processInstanceDTO)
                .stepPlans(getStepPlanDTOs(instance.getStepPlanInstances()))
                .build();
    }

    private Map<String, List<StepPlanDTO>> getStepPlanDTOs(ArrayListMultimap<String, StepPlanInstance> stepPlanInstances) {
        Map<String, Collection<StepPlanInstance>> map = stepPlanInstances.asMap();
        return map.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(),
                e -> {
                    Collection<StepPlanInstance> value = e.getValue();
                    return value.stream().map(this::getStepPlanDTO).collect(Collectors.toList());
                }));
    }

    private StepPlanDTO getStepPlanDTO(StepPlanInstance instance) {
        return StepPlanDTO.builder()
                .stepCode(instance.getStepCode())
                .stepInstanceId(instance.getStepInstanceId())
                .processInstanceId(instance.getProcessInstanceId())
                .status(instance.getStatus())
                .plannedMeasurements(getMeasurementInstanceDTOs(instance.getPlannedMeasurements()))
                .build();
    }

    private List<Measurement> getMeasurementInstanceDTOs(List<MeasurementInstance> plannedMeasurements) {
        if (plannedMeasurements == null || plannedMeasurements.isEmpty())
            return null;

        return plannedMeasurements.stream().map(this::getMeasurementInstanceDTO).collect(Collectors.toList());
    }

    private Measurement getMeasurementInstanceDTO(MeasurementInstance instance) {
        return Measurement.builder()
                .id(instance.getId())
                .tenant(instance.getTenant())
                .stepCode(instance.getStepCode())
                .stepInstanceId(instance.getStepInstanceId())
                .measuredAt(instance.getMeasuredAt())
                .metadata(instance.getMetadata())
                .code(instance.getCode())
                .unit(instance.getUnit())
                .createdOn(instance.getCreatedOn())
                .type(instance.getType())
                .value(instance.getValue())
                .processInstanceId(instance.getProcessInstanceId())
                .createdOn(instance.getCreatedOn())
                .build();
    }

    private ProcessInstanceDTO getProcessInstanceDTO(ProcessInstance processInstance) {

        final List<StepInstanceDTO> stepInstanceDTOS = processInstance.getSteps().stream().map(this::getStepInstanceDTO).collect(Collectors.toList());

        return ProcessInstanceDTO.builder()
                .entityId(processInstance.getEntityId())
                .active(processInstance.isActive())
                .categoryCode(processInstance.getCategoryCode())
                .processCode(processInstance.getProcessCode())
                .processType(processInstance.getProcessType())
                .complete(processInstance.isComplete())
                .entityType(processInstance.getEntityType())
                .id(processInstance.getId())
                .status(processInstance.getStatus())
                .subCategoryCode(processInstance.getSubCategoryCode())
                .tenant(processInstance.getTenant())
                .valid(processInstance.isValid())
                .version(processInstance.getVersion())
                .metadata(processInstance.getMetadata())
                .steps(stepInstanceDTOS)
                .build();
    }

    private StepInstanceDTO getStepInstanceDTO(StepInstance instance) {
        return StepInstanceDTO.builder()
                .id(instance.getId())
                .tenant(instance.getTenant())
                .status(instance.getStatus())
                .functionalCtx(instance.getFunctionalCtx())
                .groupCode(instance.getGroupCode())
                .version(instance.getVersion())
                .stepCode(instance.getStepCode())
                .locationCode(instance.getLocationCode())
                .metadata(instance.getMetadata())
                .processInstanceId(instance.getProcessInstanceId())
                .createdOn(instance.getCreatedOn())
                .build();
    }
}
