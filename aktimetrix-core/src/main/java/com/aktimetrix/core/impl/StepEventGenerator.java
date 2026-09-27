package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.EventGenerator;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.StepInstanceDTO;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public class StepEventGenerator implements EventGenerator {

    private final StepInstance stepInstance;
    private final String eventCode;

    public StepEventGenerator(StepInstance stepInstance) {
        this(stepInstance, "CREATED");
    }

    /**
     * @param eventCode what happened to the step: CREATED, STARTED, COMPLETED or OVERDUE
     */
    public StepEventGenerator(StepInstance stepInstance, String eventCode) {
        this.stepInstance = stepInstance;
        this.eventCode = eventCode;
    }

    /**
     * Generate the Events
     *
     * @return Event
     */
    @Override
    public Event<StepInstanceDTO, Void> generate() {
        return getStepEvent(this.stepInstance);
    }

    private Event<StepInstanceDTO, Void> getStepEvent(StepInstance instance) {
        Event<StepInstanceDTO, Void> event = new Event<>();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("Step_Event");
        event.setEventCode(eventCode);
        event.setEventName("Step Instance " + eventCode + " Event");
        event.setEventTime(ZonedDateTime.now());
        event.setEventUTCTime(LocalDateTime.now(ZoneOffset.UTC));
        event.setEntityId(String.valueOf(instance.getId()));
        event.setEntityType("com.aktimetrix.step.instance");
        event.setSource("ProcessManager");
        event.setTenantKey(instance.getTenant());
        event.setEntity(getStepInstanceDTO(instance));
        return event;
    }

    static StepInstanceDTO getStepInstanceDTO(StepInstance instance) {
        return StepInstanceDTO.builder()
                .id(instance.getId().toString())
                .tenant(instance.getTenant())
                .status(instance.getStatus())
                .functionalCtx(instance.getFunctionalCtx())
                .groupCode(instance.getGroupCode())
                .version(instance.getVersion())
                .stepCode(instance.getStepCode())
                .locationCode(instance.getLocationCode())
                .metadata(instance.getMetadata())
                .processInstanceId(instance.getProcessInstanceId().toString())
                .createdOn(instance.getCreatedOn())
                .plannedAt(instance.getPlannedAt())
                .lateAfter(instance.getLateAfter())
                .expectedAt(instance.getExpectedAt())
                .sequence(instance.getSequence())
                .actualAt(instance.getActualAt())
                .timeliness(instance.getTimeliness())
                .build();
    }
}
