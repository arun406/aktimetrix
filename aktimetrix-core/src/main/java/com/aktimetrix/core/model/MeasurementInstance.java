package com.aktimetrix.core.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Document(collection = "measurementInstances")
@ToString
public class MeasurementInstance {

    private String tenant;
    @Id
    private String id;
    private String processInstanceId;
    private String stepInstanceId;
    private String stepCode;
    private String code;
    private String value;
    private String unit;
    private String type;
    private String measuredAt;
    private Map<String, Object> metadata;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("measuredOn")
    private ZonedDateTime createdOn;

    public MeasurementInstance() {
        this.createdOn = ZonedDateTime.now();
    }

    /**
     * @param tenant
     * @param code
     * @param value
     * @param processInstanceId
     * @param stepInstanceId
     */
    public MeasurementInstance(String tenant, String code, String value, String unit, String processInstanceId,
                               String stepInstanceId, String stepCode, String type, String measuredAt, ZonedDateTime createdOn) {
        this.tenant = tenant;
        this.code = code;
        this.value = value;
        this.stepCode = stepCode;
        this.processInstanceId = processInstanceId;
        this.stepInstanceId = stepInstanceId;
        this.unit = unit;
        this.createdOn = ZonedDateTime.now();
        this.type = type;
        this.measuredAt = measuredAt;
        this.createdOn = createdOn;
    }

}
