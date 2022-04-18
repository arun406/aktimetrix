package com.aktimetrix.service.meter.core.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Data
@Document(collection = "measurementInstances")
public class MeasurementInstance {

    private String tenant;
    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @JsonIgnore
    private ObjectId processInstanceId;

    @JsonIgnore
    private ObjectId stepInstanceId;

    @JsonIgnore
    private String stepCode;

    private String code;
    private String value;
    private String unit;
    private String type;
    private String measuredAt;

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
    public MeasurementInstance(String tenant, String code, String value, String unit, ObjectId processInstanceId,
                               ObjectId stepInstanceId, String stepCode, String type, String measuredAt, ZonedDateTime createdOn) {
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
