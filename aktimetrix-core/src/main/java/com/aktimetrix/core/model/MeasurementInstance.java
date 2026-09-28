package com.aktimetrix.core.model;

import com.aktimetrix.core.api.Conformance;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.ZonedDateTime;

@Data
@Document(collection = "measurement-instance")
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
    /**
     * For an actual measurement: the planned value it is compared with, if one was planned.
     */
    private String plannedValue;
    /**
     * For an actual measurement: actual minus planned; a number, or an ISO-8601 duration for {@code TIME}.
     */
    private String deviation;
    /**
     * For an actual measurement with a tolerance: whether the deviation is within it.
     */
    private Conformance conformance;

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
