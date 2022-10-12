package com.aktimetrix.core.impl.generator;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.EventGenerator;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.StepInstanceDTO;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Component("StepEventGenerator")
public class StepEventGenerator implements EventGenerator<StepInstanceDTO, Void> {

    public static final String STEP_INSTANCE_ENTITY_TYPE = "com.aktimetrix.step.instance";

    /**
     * Generate the Events
     *
     * @return Event
     */
    @Override
    public Event<StepInstanceDTO, Void> generate(Object... object) {
        return getStepEvent((StepInstance) object[0]);
    }

    private Event<StepInstanceDTO, Void> getStepEvent(StepInstance instance) {
        Event<StepInstanceDTO, Void> event = new Event<>();
        event.setEventId(UUID.randomUUID().toString().replace("-", ""));
        event.setEventType(Constants.STEP_EVENT);
        event.setEventCode(instance.getStatus()); // Created, Completed
        ZonedDateTime eventTime = instance.getModifiedOn() != null ? instance.getModifiedOn().atZone(ZoneOffset.UTC) : instance.getCreatedOn().atZone(ZoneOffset.UTC);
        event.setEventTime(eventTime);  // Created Time/ Modified Time in UTC format
        event.setEntityId(String.valueOf(instance.getId()));
        event.setEntityType(STEP_INSTANCE_ENTITY_TYPE);
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
