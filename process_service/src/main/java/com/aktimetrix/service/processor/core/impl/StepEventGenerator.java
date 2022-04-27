package com.aktimetrix.service.processor.core.impl;

import com.aktimetrix.service.processor.core.api.EventGenerator;
import com.aktimetrix.service.processor.core.model.StepInstance;
import com.aktimetrix.service.processor.core.transferobjects.Event;
import com.aktimetrix.service.processor.core.transferobjects.StepInstanceDTO;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public class StepEventGenerator implements EventGenerator {

    private final StepInstance stepInstance;

    public StepEventGenerator(StepInstance stepInstance) {
        this.stepInstance = stepInstance;
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
        event.setEventCode("CREATED");
        event.setEventName("Step Instance Created Event");
        event.setEventTime(ZonedDateTime.now());
        event.setEventUTCTime(LocalDateTime.now(ZoneOffset.UTC));
        event.setEntityId(String.valueOf(instance.getId()));
        event.setEntityType("com.aktimetrix.step.instance");
        event.setSource("ProcessManager");
        event.setTenantKey(instance.getTenant());
        event.setEntity(getStepInstanceDTO(instance));
        return event;
    }

    private StepInstanceDTO getStepInstanceDTO(StepInstance instance) {
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
                .build();
    }
}
