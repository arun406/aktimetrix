package com.aktimetrix.service.meter.core.transferobjects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;

@Data
@ToString
@Builder
public class Measurement implements Serializable {

    private String tenant;
    private String id;
    private String processInstanceId;
    private String stepInstanceId;
    private String stepCode;
    private String code;
    private String value;
    private String unit;
    private String type;
    private String measuredAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSSZ")
    @JsonProperty("measuredOn")
    private ZonedDateTime createdOn;
    private Map<String, Object> metadata;


    /**
     * Constructor
     *
     * @param tenant
     * @param id
     * @param processInstanceId
     * @param stepInstanceId
     * @param stepCode
     * @param code
     * @param value
     * @param unit
     * @param type
     * @param measuredAt
     * @param createdOn
     */
    public Measurement(String tenant, String id, String processInstanceId, String stepInstanceId, String stepCode,
                       String code, String value, String unit, String type, String measuredAt, ZonedDateTime createdOn) {
        this.tenant = tenant;
        this.id = id;
        this.processInstanceId = processInstanceId;
        this.stepInstanceId = stepInstanceId;
        this.stepCode = stepCode;
        this.code = code;
        this.value = value;
        this.unit = unit;
        this.type = type;
        this.measuredAt = measuredAt;
        this.createdOn = createdOn;
    }

    /**
     * Constructor
     *
     * @param tenant
     * @param id
     * @param processInstanceId
     * @param stepInstanceId
     * @param stepCode
     * @param code
     * @param value
     * @param unit
     * @param type
     * @param measuredAt
     * @param createdOn
     * @param metadata
     */
    public Measurement(String tenant, String id, String processInstanceId, String stepInstanceId, String stepCode,
                       String code, String value, String unit, String type, String measuredAt, ZonedDateTime createdOn,
                       Map<String, Object> metadata) {
        this(tenant, id, processInstanceId, stepInstanceId, stepCode,
                code, value, unit, type, measuredAt, createdOn);
        this.metadata = metadata;
    }
}
