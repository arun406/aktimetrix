package com.aktimetrix.core.transferobjects;

import com.aktimetrix.core.api.Timeliness;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepInstanceDTO implements Serializable {

    private String id;
    private String processInstanceId;
    private String stepCode;
    /**
     * Name of the step, from its definition.
     */
    private String stepName;
    /**
     * Whether the process can complete without the step.
     */
    private boolean optional;
    private String locationCode;
    private String groupCode;
    private String status;
    private String version;
    private String functionalCtx;
    private Map<String, Object> metadata;
    private String tenant;
    private LocalDateTime createdOn;
    private LocalDateTime plannedAt;
    private LocalDateTime lateAfter;
    private LocalDateTime expectedAt;
    private int sequence;
    private LocalDateTime actualAt;
    private Timeliness timeliness;
}
