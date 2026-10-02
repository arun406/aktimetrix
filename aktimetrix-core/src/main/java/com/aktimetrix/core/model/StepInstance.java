package com.aktimetrix.core.model;

import com.aktimetrix.core.api.Timeliness;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.time.Instant;
import java.util.Map;

@Data
public class StepInstance {

    private String tenant;
    @Id
    private String id;
    private String processInstanceId;
    private String stepCode;
    /**
     * Position of the step in its process, from 0.
     */
    private int sequence;
    private String locationCode;
    private String groupCode;
    private String status;
    private String version;
    private String functionalCtx;
    private Map<String, Object> metadata;
    private Instant createdOn;
    /**
     * When the step should happen: the planned TIME measurement computed by its meter, if any.
     */
    private Instant plannedAt;
    /**
     * When the step counts as late: {@code plannedAt} plus the step definition's tolerance.
     */
    private Instant lateAfter;
    /**
     * Forecast of when the step will happen, when an earlier step ran late.
     */
    private Instant expectedAt;
    /**
     * When the step actually completed.
     */
    private Instant actualAt;
    /**
     * How the step compares with its plan; {@code null} until it can be judged.
     */
    private Timeliness timeliness;
    /**
     * Whether the step completed without its start event: a step with start and end events whose end arrived first,
     * a sign that a source system missed or delayed an event.
     */
    private boolean startMissing;
    /**
     * How many times the step has completed: 1 once it happened, more for a repeatable step that happened again.
     */
    private int attempts;
    /**
     * When the step last completed; differs from {@code actualAt} for a repeatable step that happened again.
     */
    private Instant lastAttemptAt;
    /**
     * Incremented on every save; a save based on a stale copy fails instead of overwriting a newer state.
     */
    @Version
    @JsonIgnore
    private Long revision;
    /**
     * When the alarm set for this instance's deadline is due; {@code null} when it has none. Kept so that a save that
     * does not move the deadline writes no alarm.
     */
    @JsonIgnore
    private Instant alarmAt;

    public StepInstance() {
        super();
    }

    /**
     * @param tenant
     * @param stepCode
     * @param processInstanceId
     * @param groupCode
     * @param functionalCtx
     * @param version
     * @param status
     * @param createdOn
     */
    public StepInstance(String tenant, String stepCode, String processInstanceId, String groupCode,
                        String functionalCtx, String version, String status, Instant createdOn) {
        this.tenant = tenant;
        this.stepCode = stepCode;
        this.processInstanceId = processInstanceId;
        this.groupCode = groupCode;
        this.functionalCtx = functionalCtx;
        this.version = version;
        this.status = status;
        this.createdOn = createdOn;
    }
}
