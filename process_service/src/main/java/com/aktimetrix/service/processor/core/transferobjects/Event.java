package com.aktimetrix.service.processor.core.transferobjects;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

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
}
