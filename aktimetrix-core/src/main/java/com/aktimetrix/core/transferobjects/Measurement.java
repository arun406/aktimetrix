package com.aktimetrix.core.transferobjects;

import com.aktimetrix.core.api.Conformance;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Map;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private String plannedValue;
    private String deviation;
    private Conformance conformance;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSSZ")
    @JsonProperty("measuredOn")
    private ZonedDateTime createdOn;
    private Map<String, Object> metadata;
}
