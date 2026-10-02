package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.Alarm;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.store.AlarmStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DeadlineAlarmsTest {

    private static final Instant DEADLINE = LocalDateTime.of(2022, 5, 23, 12, 0).toInstant(ZoneOffset.UTC);

    @Mock
    private AlarmStore store;
    @InjectMocks
    private DeadlineAlarms alarms;

    @Test
    void anOpenStepWithADeadlineGetsAnAlarmAtIt() {
        StepInstance step = step();
        assertThat(DeadlineAlarms.needsAlarm(step)).isTrue();

        alarms.reconcile(step);

        ArgumentCaptor<Alarm> alarm = ArgumentCaptor.forClass(Alarm.class);
        verify(store).schedule(alarm.capture());
        assertThat(alarm.getValue().getId()).isEqualTo("STEP:s1");
        assertThat(alarm.getValue().getDueAt()).isEqualTo(DEADLINE);
        assertThat(alarm.getValue().getProcessInstanceId()).isEqualTo("p1");
        assertThat(step.getAlarmAt()).isEqualTo(DEADLINE);
        assertThat(DeadlineAlarms.needsAlarm(step)).isFalse();
    }

    @Test
    void aSaveThatDoesNotMoveTheDeadlineWritesNoAlarm() {
        StepInstance step = step();
        step.setAlarmAt(DEADLINE);

        alarms.reconcile(step);

        verifyNoInteractions(store);
    }

    @Test
    void aMovedDeadlineMovesTheAlarm() {
        StepInstance step = step();
        step.setAlarmAt(DEADLINE.minus(Duration.ofHours(1)));

        alarms.reconcile(step);

        ArgumentCaptor<Alarm> alarm = ArgumentCaptor.forClass(Alarm.class);
        verify(store).schedule(alarm.capture());
        assertThat(alarm.getValue().getDueAt()).isEqualTo(DEADLINE);
    }

    @Test
    void aCompletedStepHasItsAlarmCancelled() {
        StepInstance step = step();
        step.setAlarmAt(DEADLINE);
        step.setStatus(Constants.STATUS_COMPLETED);

        alarms.reconcile(step);

        verify(store).cancel("STEP:s1");
        assertThat(step.getAlarmAt()).isNull();
    }

    @Test
    void anOverdueProcessHasItsAlarmCancelled() {
        ProcessInstance process = new ProcessInstance();
        process.setId("p1");
        process.setLateAfter(DEADLINE);
        assertThat(DeadlineAlarms.needsAlarm(process)).isTrue();
        process.setAlarmAt(DEADLINE);
        process.setTimeliness(Timeliness.OVERDUE);

        alarms.reconcile(process);

        verify(store).cancel("PROCESS:p1");
        assertThat(process.getAlarmAt()).isNull();
    }

    private static StepInstance step() {
        StepInstance step = new StepInstance();
        step.setId("s1");
        step.setProcessInstanceId("p1");
        step.setStatus(Constants.STATUS_CREATED);
        step.setLateAfter(DEADLINE);
        return step;
    }
}
