package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.EventGenerator;
import com.aktimetrix.core.api.PublishedEvents;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.EventContext;
import com.aktimetrix.core.transferobjects.StepInstanceDTO;

/**
 * Generates a {@code Step_Event}: the step instance, and its {@link EventContext}.
 */
public class StepEventGenerator implements EventGenerator {

    private final StepInstance stepInstance;
    private final String eventCode;
    private final EventContext context;
    private final ProcessDefinition definition;

    /**
     * @param eventCode  what happened to the step; see {@link PublishedEvents.Step}
     * @param definition the definition the step's process follows, for the step's name; may be {@code null}
     */
    public StepEventGenerator(StepInstance stepInstance, String eventCode, EventContext context,
                              ProcessDefinition definition) {
        this.stepInstance = stepInstance;
        this.eventCode = eventCode;
        this.context = context;
        this.definition = definition;
    }

    @Override
    public Event<StepInstanceDTO, EventContext> generate() {
        return EventEnvelopes.envelope(stepInstance.getTenant(), PublishedEvents.Step.TYPE,
                PublishedEvents.Step.ENTITY_TYPE, eventCode, "Step", stepInstance.getId(),
                dto(stepInstance, definition), context);
    }

    static StepInstanceDTO dto(StepInstance instance, ProcessDefinition definition) {
        final StepDefinition step = stepDefinition(definition, instance.getStepCode());
        return StepInstanceDTO.builder()
                .id(instance.getId())
                .tenant(instance.getTenant())
                .processInstanceId(instance.getProcessInstanceId())
                .stepCode(instance.getStepCode())
                .stepName(step == null ? null : step.getStepName())
                .optional(step != null && "Y".equalsIgnoreCase(step.getOptionalInd()))
                .sequence(instance.getSequence())
                .status(instance.getStatus())
                .functionalCtx(instance.getFunctionalCtx())
                .groupCode(instance.getGroupCode())
                .version(instance.getVersion())
                .locationCode(instance.getLocationCode())
                .metadata(instance.getMetadata())
                .createdOn(instance.getCreatedOn())
                .plannedAt(instance.getPlannedAt())
                .lateAfter(instance.getLateAfter())
                .expectedAt(instance.getExpectedAt())
                .actualAt(instance.getActualAt())
                .timeliness(instance.getTimeliness())
                .build();
    }

    private static StepDefinition stepDefinition(ProcessDefinition definition, String stepCode) {
        if (definition == null || definition.getSteps() == null) {
            return null;
        }
        return definition.getSteps().stream().filter(s -> s != null && stepCode.equals(s.getStepCode()))
                .findFirst().orElse(null);
    }
}
