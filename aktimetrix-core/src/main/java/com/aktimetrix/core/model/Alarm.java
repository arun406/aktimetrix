package com.aktimetrix.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * An alarm at the deadline of a step or process: if the step or process is still open when it fires, it is marked
 * overdue. There is at most one alarm per step or process; its id is derived from the target, see {@link #idOf}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alarm {

    public static final String STEP = "STEP";
    public static final String PROCESS = "PROCESS";

    @Id
    private String id;
    private String tenant;
    /**
     * {@value #STEP} or {@value #PROCESS}.
     */
    private String kind;
    /**
     * The step or process instance the alarm watches.
     */
    private String targetId;
    private String processInstanceId;
    /**
     * When the alarm is due: the target's deadline ({@code lateAfter}), in {@code aktimetrix.time-zone}.
     */
    private LocalDateTime dueAt;
    /**
     * Until when an instance has claimed the alarm; {@code null} while unclaimed.
     */
    private Instant lockedUntil;
    private int attempts;

    public static String idOf(String kind, String targetId) {
        return kind + ":" + targetId;
    }

    public static Alarm of(String kind, String tenant, String targetId, String processInstanceId, LocalDateTime dueAt) {
        return new Alarm(idOf(kind, targetId), tenant, kind, targetId, processInstanceId, dueAt, null, 0);
    }
}
