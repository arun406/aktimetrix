package com.aktimetrix.core.api;

import com.aktimetrix.core.transferobjects.Event;

/**
 * core interface for generating the events. Event will mainly contain entity and optional eventDetails
 *
 * @param <U>
 * @param <V>
 */
public interface EventGenerator<U, V> {
    /**
     * Generate the Events
     *
     * @return Event
     */
    Event<U, V> generate(Object... object);
}
