package com.aktimetrix.service.meter.core.transferobjects;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Event<U, V> {
    private String tenantKey;
    private String eventId;
    private String eventType;
    private String eventCode;
    private String eventTime;
    private String source;
    private String entityId;
    private String entityType;
    private U entity;
    private V eventDetails;
}
