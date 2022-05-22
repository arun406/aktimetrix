package com.aktimetrix.service.processor.ciq.cdmpc.event.handler;

import com.aktimetrix.core.api.EventType;
import com.aktimetrix.core.event.handler.AbstractEventHandler;
import com.aktimetrix.core.impl.DefaultProcessDefinitionProvider;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.stereotypes.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author arun kumar k
 */
@Component
@EventHandler(eventType = EventType.BKD)
@Slf4j
public class BKDEventHandler extends AbstractEventHandler {
    /* @Override
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
    }*/
}
