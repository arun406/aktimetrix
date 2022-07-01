package com.aktimetrix.core.transferobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Map;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
    @JsonProperty("measuredOn")
    private ZonedDateTime createdOn;
    private Map<String, Object> metadata;

}
