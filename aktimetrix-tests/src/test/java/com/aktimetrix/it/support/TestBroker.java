package com.aktimetrix.it.support;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A message broker started for a test, and a client of it.
 */
public interface TestBroker extends AutoCloseable {

    /**
     * Spring properties that connect an Aktimetrix application to this broker.
     */
    Map<String, Object> properties();

    /**
     * Publishes a business event, as a source system would.
     */
    void send(String destination, String key, String payload);

    /**
     * The messages published to an outbound binding (or to the dead-letter channel, {@code events-topic.dlq}), from
     * the first, waiting until they are {@code enough} or 20 seconds have passed.
     */
    List<String> received(String destination, Predicate<List<String>> enough);

    default List<String> received(String destination, int expected) {
        return received(destination, messages -> messages.size() >= expected);
    }

    @Override
    void close();
}
