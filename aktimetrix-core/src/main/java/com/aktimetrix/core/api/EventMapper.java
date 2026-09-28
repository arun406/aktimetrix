package com.aktimetrix.core.api;

import com.aktimetrix.core.transferobjects.Event;

import java.util.Map;

/**
 * Turns a message from the inbound topic into an Aktimetrix {@link Event}, so that source systems can keep publishing
 * their own event format.
 * <p>
 * By default, messages are expected in the Aktimetrix event envelope. To accept another format, declare one bean of
 * this type; it replaces the default:
 * <pre>{@code
 * @Bean
 * EventMapper shopEvents(ObjectMapper json) {
 *     return (payload, headers) -> {
 *         JsonNode order = json.readTree(payload);
 *         Event<Object, Object> event = Event.of("AA", "ORDER_" + order.get("status").asText() + "_EVENT",
 *                 "com.ecom.order", order.get("id").asText(), ZonedDateTime.parse(order.get("updatedAt").asText()));
 *         event.setEntity(json.convertValue(order, Map.class));
 *         return event;
 *     };
 * }
 * }</pre>
 */
@FunctionalInterface
public interface EventMapper {

    /**
     * @param payload the message as received
     * @param headers the message headers, such as the message key
     * @return the event, with at least its tenant, event code and entity id; or {@code null} to ignore the message,
     * for example an event type that is not monitored
     * @throws Exception when the message cannot be understood: it is then sent to the dead-letter topic
     */
    Event<?, ?> map(String payload, Map<String, Object> headers) throws Exception;
}
