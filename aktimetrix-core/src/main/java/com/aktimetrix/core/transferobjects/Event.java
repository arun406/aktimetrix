package com.aktimetrix.core.transferobjects;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * A business event, as Aktimetrix processes it. Inbound messages are read into this envelope, directly or by an
 * {@code EventMapper}; outbound process, step and measurement events use it too.
 * <p>
 * Required inbound: {@code tenantKey}, {@code eventCode}, {@code entityId}, and {@code entityType} matching the process
 * definition. {@code eventTime} (or {@code eventUTCTime}) is when it happened in the business, and {@code entity} the
 * domain object, which becomes metadata.
 *
 * @param <U> type of the entity
 * @param <V> type of the event details
 */
@Data
@ToString
public class Event<U, V> {
    private String tenantKey;
    private String eventId;
    private String eventType;
    private String eventName;
    private String eventCode;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private ZonedDateTime eventTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventUTCTime;
    private String source;
    private String entityId;
    private String entityType;
    private U entity;
    private V eventDetails;

    /**
     * An event with the fields Aktimetrix needs, for use in an {@code EventMapper}; set {@code entity} for the data
     * that becomes metadata.
     *
     * @param tenantKey  the tenant
     * @param eventCode  what happened, e.g. {@code ORDER_SHIPPED_EVENT}
     * @param entityType the type of business entity, which must match the process definition's
     * @param entityId   the business entity, e.g. the order number
     * @param eventTime  when it happened in the business
     */
    public static Event<Object, Object> of(String tenantKey, String eventCode, String entityType, String entityId,
                                           ZonedDateTime eventTime) {
        final Event<Object, Object> event = new Event<>();
        event.setTenantKey(tenantKey);
        event.setEventCode(eventCode);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setEventTime(eventTime);
        return event;
    }
}
