package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.Alarm;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.store.AlarmStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * Keeps the alarm of a step or process in line with its deadline, as it is saved: an open instance with a deadline
 * that is not overdue yet has an alarm at that deadline; any other has none. The instance's {@code alarmAt} records the
 * alarm set, so a save that does not move the deadline writes no alarm.
 */
@Component
@RequiredArgsConstructor
public class DeadlineAlarms {

    private static final Set<String> CLOSED_STEP = Set.of(Constants.STATUS_COMPLETED, Constants.STATUS_CANCELLED,
            Constants.STATUS_SKIPPED);

    private final AlarmStore alarms;

    /**
     * Sets, moves or cancels the step's alarm, before the step is saved. The step must have an id.
     */
    public void reconcile(StepInstance step) {
        final LocalDateTime due = !CLOSED_STEP.contains(step.getStatus()) && step.getTimeliness() != Timeliness.OVERDUE
                ? step.getLateAfter() : null;
        if (!Objects.equals(due, step.getAlarmAt())) {
            apply(Alarm.STEP, step.getTenant(), step.getId(), step.getProcessInstanceId(), due);
            step.setAlarmAt(due);
        }
    }

    /**
     * Sets, moves or cancels the process's alarm, before the process is saved. The process must have an id.
     */
    public void reconcile(ProcessInstance process) {
        final LocalDateTime due = !process.isComplete() && process.getTimeliness() != Timeliness.OVERDUE
                ? process.getLateAfter() : null;
        if (!Objects.equals(due, process.getAlarmAt())) {
            apply(Alarm.PROCESS, process.getTenant(), process.getId(), process.getId(), due);
            process.setAlarmAt(due);
        }
    }

    /**
     * Whether the instance needs an alarm it does not have yet: then it must be saved again once it has an id.
     */
    public static boolean needsAlarm(StepInstance step) {
        return step.getLateAfter() != null && step.getAlarmAt() == null && !CLOSED_STEP.contains(step.getStatus())
                && step.getTimeliness() != Timeliness.OVERDUE;
    }

    public static boolean needsAlarm(ProcessInstance process) {
        return process.getLateAfter() != null && process.getAlarmAt() == null && !process.isComplete()
                && process.getTimeliness() != Timeliness.OVERDUE;
    }

    private void apply(String kind, String tenant, String targetId, String processInstanceId, LocalDateTime due) {
        if (due == null) {
            alarms.cancel(Alarm.idOf(kind, targetId));
        } else {
            alarms.schedule(Alarm.of(kind, tenant, targetId, processInstanceId, due));
        }
    }
}
