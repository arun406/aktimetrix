package com.aktimetrix.core.model;

import com.aktimetrix.core.api.Conformance;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.ZonedDateTime;

@Data
public class MeasurementInstance {

    private String tenant;
    @Id
    private String id;

    @JsonIgnore
    private String processInstanceId;

    @JsonIgnore
    private String stepInstanceId;

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
    /**
     * {@code true} for a reading taken while its step was still in progress; the final actual is recorded when the step
     * completes.
     */
    private boolean interim;
    /**
     * For a metric computed from other measurements: its expression, e.g. {@code FUEL / DISTANCE}.
     */
    private String derivedFrom;

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
