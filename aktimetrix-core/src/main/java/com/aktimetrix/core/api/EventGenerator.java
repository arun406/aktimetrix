package com.aktimetrix.core.api;

import com.aktimetrix.core.transferobjects.Event;

public interface EventGenerator {
    /**
     * Generate the Events
     *
     * @return Event
     */
    Event<?, ?> generate();
}
