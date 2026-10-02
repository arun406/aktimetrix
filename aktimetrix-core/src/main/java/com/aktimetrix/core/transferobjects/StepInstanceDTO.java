package com.aktimetrix.core.transferobjects;

import com.aktimetrix.core.api.Timeliness;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
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
    private Instant createdOn;
    private Instant plannedAt;
    private Instant lateAfter;
    private Instant expectedAt;
    private int sequence;
    private Instant actualAt;
    private Timeliness timeliness;
    private boolean startMissing;
    private int attempts;
    private Instant lastAttemptAt;
}
