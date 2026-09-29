package com.aktimetrix.core.service;

import com.aktimetrix.core.api.PublishedEvents;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.transferobjects.EventContext;
import com.aktimetrix.core.transferobjects.EventContext.BusinessEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Builds the {@link EventContext} of the events Aktimetrix publishes: the business entity and process an instance
 * belongs to, its revision, and the cause and business time of the current unit of work.
 */
@Component
@RequiredArgsConstructor
public class PublishedEventContexts {

    private final ProcessInstanceService processInstanceService;
    private final Clock clock;

    public EventContext of(ProcessInstance process) {
        return base(process).revision(process.getRevision()).build();
    }

    public EventContext of(StepInstance step) {
        return base(process(step.getTenant(), step.getProcessInstanceId()))
                .processInstanceId(step.getProcessInstanceId())
                .stepCode(step.getStepCode())
                .stepInstanceId(step.getId())
                .revision(step.getRevision())
                .build();
    }

    public EventContext of(MeasurementInstance measurement) {
        return base(process(measurement.getTenant(), measurement.getProcessInstanceId()))
                .processInstanceId(measurement.getProcessInstanceId())
                .stepCode(measurement.getStepCode())
                .stepInstanceId(measurement.getStepInstanceId())
                .build();
    }

    /**
     * The definition the process follows, for the names and flags of its steps; {@code null} if unknown.
     */
    public ProcessDefinition definitionOf(String tenant, String processInstanceId) {
        final ProcessInstance process = process(tenant, processInstanceId);
        return process == null ? null : process.getDefinition();
    }

    private ProcessInstance process(String tenant, String processInstanceId) {
        return processInstanceId == null ? null : processInstanceService.getProcessInstance(tenant, processInstanceId);
    }

    private EventContext.EventContextBuilder base(ProcessInstance process) {
        final EventContext.EventContextBuilder context = EventContext.builder()
                .schemaVersion(PublishedEvents.SCHEMA_VERSION)
                .occurredAt(ProcessingContext.occurredAt(() -> LocalDateTime.now(clock)))
                .cause(ProcessingContext.cause());
        if (process != null) {
            context.businessEntity(new BusinessEntity(process.getEntityType(), process.getEntityId()))
                    .processCode(process.getProcessCode())
                    .processInstanceId(process.getId())
                    .definitionRevision(process.getDefinitionRevision());
        }
        return context;
    }
}
