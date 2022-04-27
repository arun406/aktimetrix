package com.aktimetrix.service.processor.core.impl;

import com.aktimetrix.service.processor.core.api.EventGenerator;
import com.aktimetrix.service.processor.core.model.ProcessInstance;
import com.aktimetrix.service.processor.core.transferobjects.Event;
import com.aktimetrix.service.processor.core.transferobjects.ProcessInstanceDTO;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public class ProcessEventGenerator implements EventGenerator {

    private final ProcessInstance processInstance;

    public ProcessEventGenerator(ProcessInstance processInstance) {
        this.processInstance = processInstance;
    }

    /**
     * Generate the Events
     *
     * @return Event
     */
    @Override
    public Event<ProcessInstanceDTO, Void> generate() {
        return getProcessEvent(processInstance);
    }

    private Event<ProcessInstanceDTO, Void> getProcessEvent(ProcessInstance processInstance) {
        Event<ProcessInstanceDTO, Void> event = new Event<>();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("Process_Event");
        event.setEventCode("CREATED");
        event.setEventName("Process Instance Created Event");
        event.setEventTime(ZonedDateTime.now());
        event.setEventUTCTime(LocalDateTime.now(ZoneOffset.UTC));
        event.setEntityId(String.valueOf(processInstance.getId()));
        event.setEntityType("com.aktimetrix.process.instance");
        event.setSource("ProcessManager");
        event.setTenantKey(processInstance.getTenant());
        event.setEntity(getProcessInstanceDTO(processInstance));
        return event;
    }

    private ProcessInstanceDTO getProcessInstanceDTO(ProcessInstance processInstance) {
        return ProcessInstanceDTO.builder()
                .entityId(processInstance.getEntityId())
                .active(processInstance.isActive())
                .categoryCode(processInstance.getCategoryCode())
                .processCode(processInstance.getProcessCode())
                .complete(processInstance.isComplete())
                .entityType(processInstance.getEntityType())
                .id(processInstance.getId().toString())
                .status(processInstance.getStatus())
                .subCategoryCode(processInstance.getSubCategoryCode())
                .tenant(processInstance.getTenant())
                .valid(processInstance.isValid())
                .version(processInstance.getVersion())
                .metadata(processInstance.getMetadata())
                .build();
    }


}
