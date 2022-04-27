package com.aktimetrix.service.meter.core.api;


import com.aktimetrix.service.meter.core.transferobjects.Event;
import com.aktimetrix.service.meter.core.transferobjects.EventType;

/**
 * @author arun kumar kandakatla
 */
public interface EventHandler {

    EventType getType();

    /**
     * @param event
     */
    void handle(Event<?, ?> event);
}
