package com.aktimetrix.it;

import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.stereotypes.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Fails on every attempt, as a bug or an unavailable dependency would: the binder retries the event, then sends it to
 * the dead-letter channel.
 */
@Component
@EventHandler(eventType = "PARCEL_POISON")
public class PoisonEventHandler implements com.aktimetrix.core.api.EventHandler {

    @Override
    public void handle(Event<?, ?> event) {
        throw new IllegalStateException("cannot process " + event.getEntityId());
    }
}
