package com.aktimetrix.core.event.handler;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.stereotypes.EventHandler;
import org.springframework.stereotype.Component;

@Component
@EventHandler(eventType = Constants.STEP_EVENT, eventCode = Constants.STEP_CREATED)
public class StepCreatedEventHandler extends AbstractStepEventHandler {
}
