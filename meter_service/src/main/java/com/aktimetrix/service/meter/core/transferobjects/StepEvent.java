package com.aktimetrix.service.meter.core.transferobjects;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class StepEvent {
    private String tenantKey;
    private String eventId;
    private String eventName;
    private String eventType;
    private String eventCode;
    private String eventTime;
    private String eventUTCTime;
    private String source;
    private String entityId;
    private String entityType;
    private Step eventDetails;
}
