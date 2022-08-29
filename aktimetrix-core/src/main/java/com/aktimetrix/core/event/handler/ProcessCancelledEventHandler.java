package com.aktimetrix.core.event.handler;


import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.stereotypes.EventHandler;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for handing the process updated events from processor microservice.
 *
 * @author Arun.Kandakatla
 */
@Component
@EventHandler(eventType = Constants.PROCESS_EVENT, eventCode = Constants.PROCESS_CANCELLED)
public class ProcessCancelledEventHandler extends AbstractProcessEventHandler {

    /**
     * default constructor
     *
     * @param registryService
     */
    public ProcessCancelledEventHandler(RegistryService registryService) {
        super(registryService);
    }
}
