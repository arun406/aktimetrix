package com.aktimetrix.core.event.handler;

import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.stereotypes.EventHandler;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.StepInstanceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@EventHandler(eventType = "STEP_EVENT")
@Component
public class StepEventHandler extends AbstractStepEventHandler {

    final private static Logger logger = LoggerFactory.getLogger(StepEventHandler.class);

    public StepEventHandler(RegistryService registryService) {
        super(registryService);
    }

    protected String entityType(Event<?, ?> event) {
        return "process.step";
    }

    public String entityId(Event<?, ?> event) {
        final StepInstanceDTO entity = (StepInstanceDTO) event.getEntity();
        return entity.getId();
    }
}
