package com.aktimetrix.service.processor.ciq.cdmpc.event.handler;

import com.aktimetrix.core.api.EventType;
import com.aktimetrix.core.api.Registry;
import com.aktimetrix.core.event.handler.AbstractEventHandler;
import com.aktimetrix.core.impl.DefaultProcessDefinitionProvider;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.stereotypes.EventHandler;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.service.processor.ciq.cdmpc.event.transferobjects.Cargo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author arun kumar k
 */
@Component
@EventHandler(eventType = EventType.BKD)
public class BKDEventHandler extends AbstractEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(BKDEventHandler.class);

    public BKDEventHandler(DefaultProcessDefinitionProvider defaultProcessDefinitionProvider, RegistryService registryService) {
        super(defaultProcessDefinitionProvider, registryService);
    }


    @Override
    protected String entityType(Event<?, ?> event) {
        return "cargo.awb";
    }

    @Override
    public String entityId(Event<?, ?> event) {
        if (event.getEntity() != null) {
            final Cargo entity = (Cargo) event.getEntity();

            return entity.getDocumentInfo().getAwbInfo().getDocumentPrefix().concat(
                    entity.getDocumentInfo().getAwbInfo().getDocumentNumber());
        }
        return null;
    }
}
