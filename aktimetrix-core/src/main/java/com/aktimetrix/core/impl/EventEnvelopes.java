package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.PublishedEvents;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.EventContext;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * The envelope every published event shares.
 */
final class EventEnvelopes {

    private EventEnvelopes() {
    }

    static <T> Event<T, EventContext> envelope(String tenant, String type, String entityType, String code, String subject,
                                               String entityId, T entity, EventContext context) {
        final Event<T, EventContext> event = new Event<>();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(type);
        event.setEventCode(code);
        event.setEventName(subject + " " + code.toLowerCase(Locale.ROOT).replace('_', ' '));
        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        event.setEventTime(now);
        event.setEventUTCTime(LocalDateTime.ofInstant(now.toInstant(), ZoneOffset.UTC));
        event.setSource(PublishedEvents.SOURCE);
        event.setTenantKey(tenant);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setEntity(entity);
        event.setEventDetails(context);
        return event;
    }
}
