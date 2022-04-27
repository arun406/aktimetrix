package com.aktimetrix.service.meter.core.transferobjects;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Data
@ToString
public class StepMeasurementEvent implements Serializable {
    private String tenantKey;
    private String eventId;
    private String eventName;
    private String eventType;
    private String eventCode;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ssZ")
    private ZonedDateTime eventTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventUTCTime;
    private String source;
    private String entityId;
    private String entityType;

    private Measurement eventDetails;
}
