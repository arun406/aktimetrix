package com.aktimetrix.core.event.handler;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.stereotypes.EventHandler;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.StepInstanceDTO;
import org.springframework.stereotype.Component;

@Component
@EventHandler(eventType = Constants.STEP_EVENT, eventCode = Constants.STEP_CREATED, version = Constants.DEFAULT_VERSION)
public class StepCreatedEventHandler extends AbstractStepEventHandler {
    public StepCreatedEventHandler(RegistryService registryService) {
        super(registryService);
    }
}
