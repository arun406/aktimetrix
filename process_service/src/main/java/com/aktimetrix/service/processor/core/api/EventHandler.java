package com.aktimetrix.service.processor.core.api;

import com.aktimetrix.service.processor.core.transferobjects.Event;

/**
 * @author arun kumar kandakatla
 */
public interface EventHandler {

    /**
     * @param event
     */
    void handle(Event<?, ?> event);
}
