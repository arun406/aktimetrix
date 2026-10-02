package com.aktimetrix.core.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * What a {@link Notifier} is told: which entity, which process run and step, what happened, and the times behind it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    /**
     * Unique per notification, the same on every delivery attempt: the id of the published event behind it.
     */
    private String id;
    /**
     * {@value #STEP} or {@value #PROCESS}.
     */
    private String subject;
    /**
     * {@code AT_RISK}, {@code OVERDUE} or {@code LATE}.
     */
    private String condition;
    private String tenant;
    private String processCode;
    private String processInstanceId;
    private Integer run;
    private String entityType;
    private String entityId;
    /**
     * For a step: its code and instance id.
     */
    private String stepCode;
    private String stepInstanceId;
    private Instant plannedAt;
    private Instant lateAfter;
    /**
     * For a step at risk: when it is now expected.
     */
    private Instant expectedAt;
    /**
     * For a late step or process: when it completed.
     */
    private Instant actualAt;
    /**
     * When the condition arose, in business time.
     */
    private Instant occurredAt;

    public static final String STEP = "STEP";
    public static final String PROCESS = "PROCESS";
}
