package com.aktimetrix.service.planner.transferobjects;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class StepInstanceDTO implements Serializable {

    private String id;
    private String processInstanceId;
    private String stepCode;
    private String locationCode;
    private String groupCode;
    private String status;
    private String version;
    private String functionalCtx;
    private Map<String, Object> metadata;
    private String tenant;
    private LocalDateTime createdOn;
}
