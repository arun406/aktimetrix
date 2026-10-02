package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.model.Alarm;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.store.AktimetrixTransactions;
import com.aktimetrix.core.store.AlarmStore;
import com.aktimetrix.core.store.ProcessInstanceStore;
import com.aktimetrix.core.store.StepInstanceStore;
import com.aktimetrix.core.transferobjects.EventContext.Cause;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmSchedulerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2022-05-23T12:00:00Z"), ZoneOffset.UTC);
    private static final Instant NOW = LocalDateTime.of(2022, 5, 23, 12, 0).toInstant(ZoneOffset.UTC);

    @Mock
    private AlarmStore alarms;
    @Mock
    private StepInstanceStore steps;
    @Mock
    private ProcessInstanceStore processes;
    @Mock
    private StepProgressService stepProgressService;
    @Mock
    private AktimetrixTransactions transactions;

    private final AktimetrixProperties properties = new AktimetrixProperties();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private AlarmScheduler scheduler;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactions).run(any());
        properties.getAlarms().setBatchSize(2);
        scheduler = new AlarmScheduler(alarms, steps, processes, stepProgressService, transactions, properties,
                StepProgressServiceTest.metrics(registry), CLOCK);
    }

    @Test
    void anOpenStepPastItsDeadlineIsMarkedOverdueAsADeadlineCause() {
        StepInstance step = step("s1", NOW.minus(Duration.ofMinutes(3)));
        Alarm alarm = alarmOf(step);
        due(List.of(alarm));
        when(steps.findById("s1")).thenReturn(Optional.of(step));
        AtomicReference<Cause> cause = new AtomicReference<>();
        doAnswer(invocation -> {
            cause.set(ProcessingContext.cause());
            return null;
        }).when(stepProgressService).markOverdue(step, NOW);

        assertThat(scheduler.fireDueAlarms()).isEqualTo(1);
        verify(stepProgressService).markOverdue(step, NOW);
        assertThat(cause.get().getType()).isEqualTo(Cause.DEADLINE);
        assertThat(registry.get("aktimetrix.alarms.fired").counter().count()).isEqualTo(1);
    }

    @Test
    void anAlarmForAStepCompletedMeanwhileIsCancelled() {
        StepInstance step = step("s1", NOW.minus(Duration.ofMinutes(3)));
        step.setStatus(Constants.STATUS_COMPLETED);
        Alarm alarm = alarmOf(step);
        due(List.of(alarm));
        when(steps.findById("s1")).thenReturn(Optional.of(step));

        assertThat(scheduler.fireDueAlarms()).isZero();
        verify(alarms).cancel(alarm.getId());
        verify(stepProgressService, never()).markOverdue(any(), any());
    }

    @Test
    void anAlarmWhoseDeadlineMovedLaterIsSetAgain() {
        StepInstance step = step("s1", NOW.plus(Duration.ofHours(1)));
        Alarm alarm = Alarm.of(Alarm.STEP, "T1", "s1", "p1", NOW.minus(Duration.ofMinutes(3)));
        due(List.of(alarm));
        when(steps.findById("s1")).thenReturn(Optional.of(step));

        assertThat(scheduler.fireDueAlarms()).isZero();
        ArgumentCaptor<Alarm> moved = ArgumentCaptor.forClass(Alarm.class);
        verify(alarms).schedule(moved.capture());
        assertThat(moved.getValue().getDueAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
    }

    @Test
    void anAlarmThatLosesARaceIsLeftForItsLeaseAndTheBatchCarriesOn() {
        StepInstance ship = step("s1", NOW.minus(Duration.ofMinutes(3)));
        ProcessInstance order = new ProcessInstance();
        order.setId("p1");
        order.setTenant("T1");
        order.setLateAfter(NOW.minus(Duration.ofMinutes(1)));
        due(List.of(alarmOf(ship), Alarm.of(Alarm.PROCESS, "T1", "p1", "p1", NOW.minus(Duration.ofMinutes(1)))));
        when(steps.findById("s1")).thenReturn(Optional.of(ship));
        when(processes.findById("T1", "p1")).thenReturn(Optional.of(order));
        doThrow(new OptimisticLockingFailureException("stale")).when(stepProgressService).markOverdue(ship, NOW);

        assertThat(scheduler.fireDueAlarms()).isEqualTo(1);
        verify(stepProgressService).markProcessOverdue(order);
        verify(alarms, never()).cancel(any());
    }

    @Test
    void claimsBatchesUntilNoneIsFull() {
        StepInstance a = step("a", NOW.minus(Duration.ofMinutes(3)));
        StepInstance b = step("b", NOW.minus(Duration.ofMinutes(2)));
        StepInstance c = step("c", NOW.minus(Duration.ofMinutes(1)));
        when(alarms.claimDue(eq(NOW), eq(CLOCK.instant()), eq(CLOCK.instant().plusSeconds(30)), anyInt()))
                .thenReturn(List.of(alarmOf(a), alarmOf(b)), List.of(alarmOf(c)));
        when(steps.findById(any())).thenAnswer(i -> Optional.of(List.of(a, b, c).stream()
                .filter(s -> s.getId().equals(i.getArgument(0))).findFirst().orElseThrow()));

        assertThat(scheduler.fireDueAlarms()).isEqualTo(3);
    }

    private void due(List<Alarm> batch) {
        when(alarms.claimDue(eq(NOW), eq(CLOCK.instant()), eq(CLOCK.instant().plusSeconds(30)), eq(2)))
                .thenReturn(batch, List.of());
    }

    private static Alarm alarmOf(StepInstance step) {
        return Alarm.of(Alarm.STEP, step.getTenant(), step.getId(), step.getProcessInstanceId(), step.getLateAfter());
    }

    private static StepInstance step(String id, Instant lateAfter) {
        StepInstance step = new StepInstance();
        step.setId(id);
        step.setTenant("T1");
        step.setProcessInstanceId("p1");
        step.setStepCode(id.toUpperCase());
        step.setStatus(Constants.STATUS_CREATED);
        step.setLateAfter(lateAfter);
        return step;
    }
}
