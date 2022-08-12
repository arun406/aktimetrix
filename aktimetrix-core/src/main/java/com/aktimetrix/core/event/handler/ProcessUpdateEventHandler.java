package com.aktimetrix.core.event.handler;


import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.stereotypes.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for handing the process updated events from processor microservice.
 *
 * @author Arun.Kandakatla
 */
@Component
@EventHandler(eventType = Constants.PROCESS_EVENT, eventCode = Constants.PROCESS_UPDATED, version = "1.0.0")
@Slf4j
public class ProcessUpdateEventHandler extends AbstractProcessEventHandler {

    public ProcessUpdateEventHandler(RegistryService registryService) {
        super(registryService);
    }
}