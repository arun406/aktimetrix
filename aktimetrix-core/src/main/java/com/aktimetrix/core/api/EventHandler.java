package com.aktimetrix.core.api;

import com.aktimetrix.core.transferobjects.Event;

/**
 * @author arun kumar kandakatla
 */
public interface EventHandler {

    /**
     * @param event
     */
    void handle(Event<?, ?> event);
}
