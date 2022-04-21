package com.aktimetrix.service.processor.core.api;

import com.aktimetrix.service.processor.core.transferobjects.Event;

public interface EventGenerator {
    /**
     * Generate the Events
     *
     * @return Event
     */
    Event<?, ?> generate();
}
