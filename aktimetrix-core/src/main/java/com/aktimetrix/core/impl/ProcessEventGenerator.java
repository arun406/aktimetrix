package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.EventGenerator;
import com.aktimetrix.core.api.PublishedEvents;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.EventContext;
import com.aktimetrix.core.transferobjects.ProcessInstanceDTO;

import java.util.stream.Collectors;

/**
 * Generates a {@code Process_Event}: the process instance, with its steps, and its {@link EventContext}.
 */
public class ProcessEventGenerator implements EventGenerator {

    private final ProcessInstance processInstance;
    private final String eventCode;
    private final EventContext context;

    /**
     * @param eventCode what happened to the process; see {@link PublishedEvents.Process}
     */
    public ProcessEventGenerator(ProcessInstance processInstance, String eventCode, EventContext context) {
        this.processInstance = processInstance;
        this.eventCode = eventCode;
        this.context = context;
    }

    @Override
    public Event<ProcessInstanceDTO, EventContext> generate() {
        return EventEnvelopes.envelope(processInstance.getTenant(), PublishedEvents.Process.TYPE,
                PublishedEvents.Process.ENTITY_TYPE, eventCode, "Process", processInstance.getId(),
                dto(processInstance), context);
    }

    static ProcessInstanceDTO dto(ProcessInstance processInstance) {
        final ProcessDefinition definition = processInstance.getDefinition();
        return ProcessInstanceDTO.builder()
                .id(processInstance.getId())
                .tenant(processInstance.getTenant())
                .processCode(processInstance.getProcessCode())
                .processName(definition == null ? null : definition.getProcessName())
                .definitionRevision(processInstance.getDefinitionRevision())
                .entityType(processInstance.getEntityType())
                .entityId(processInstance.getEntityId())
                .categoryCode(processInstance.getCategoryCode())
                .subCategoryCode(processInstance.getSubCategoryCode())
                .status(processInstance.getStatus())
                .complete(processInstance.isComplete())
                .active(processInstance.isActive())
                .valid(processInstance.isValid())
                .version(processInstance.getVersion())
                .metadata(processInstance.getMetadata())
                .startedAt(processInstance.getStartedAt())
                .plannedAt(processInstance.getPlannedAt())
                .lateAfter(processInstance.getLateAfter())
                .endedAt(processInstance.getEndedAt())
                .timeliness(processInstance.getTimeliness())
                .steps(processInstance.getSteps().stream()
                        .map(step -> StepEventGenerator.dto(step, definition))
                        .collect(Collectors.toList()))
                .build();
    }
}
