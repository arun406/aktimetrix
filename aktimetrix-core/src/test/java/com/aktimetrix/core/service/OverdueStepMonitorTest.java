package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.repository.StepInstanceRepository;
import com.aktimetrix.core.storage.AktimetrixTransactions;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverdueStepMonitorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2022-05-23T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2022, 5, 23, 12, 0);

    @Mock
    private StepInstanceRepository stepInstanceRepository;
    @Mock
    private StepProgressService stepProgressService;
    @Mock
    private AktimetrixTransactions transactions;

    private OverdueStepMonitor monitor;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactions).run(any());
        monitor = new OverdueStepMonitor(stepInstanceRepository, stepProgressService, CLOCK, transactions);
    }

    @Test
    void marksStepsPastTheirDeadlineOverdue() {
        StepInstance deliver = step("DELIVER");
        when(stepInstanceRepository.findOverdue(NOW)).thenReturn(List.of(deliver));
        stored(deliver);

        assertThat(monitor.checkOverdueSteps()).containsExactly(deliver);
        verify(stepProgressService).markOverdue(deliver, NOW);
    }

    @Test
    void skipsAStepThatChangedSinceItWasReadAndCarriesOn() {
        StepInstance ship = step("SHIP");
        StepInstance deliver = step("DELIVER");
        when(stepInstanceRepository.findOverdue(NOW)).thenReturn(List.of(ship, deliver));
        stored(ship);
        stored(deliver);
        // e.g. another instance marked SHIP first, or its event just completed it
        doThrow(new OptimisticLockingFailureException("stale")).when(stepProgressService).markOverdue(ship, NOW);

        assertThat(monitor.checkOverdueSteps()).containsExactly(deliver);
        verify(stepProgressService).markOverdue(deliver, NOW);
    }

    @Test
    void marksTheCurrentStateOfAStepThatAnEarlierOneChanged() {
        StepInstance handover = step("HANDOVER");
        StepInstance accept = step("ACCEPT");
        when(stepInstanceRepository.findOverdue(NOW)).thenReturn(List.of(handover, accept));
        stored(handover);
        // marking HANDOVER overdue put ACCEPT at risk: a newer copy than the one found
        StepInstance acceptNow = step("ACCEPT");
        acceptNow.setId(accept.getId());
        acceptNow.setTimeliness(Timeliness.AT_RISK);
        acceptNow.setRevision(1L);
        when(stepInstanceRepository.findById(accept.getId().toHexString())).thenReturn(Optional.of(acceptNow));

        assertThat(monitor.checkOverdueSteps()).containsExactly(handover, acceptNow);
        verify(stepProgressService).markOverdue(acceptNow, NOW);
    }

    @Test
    void skipsAStepCompletedSinceItWasFound() {
        StepInstance deliver = step("DELIVER");
        when(stepInstanceRepository.findOverdue(NOW)).thenReturn(List.of(deliver));
        StepInstance completed = step("DELIVER");
        completed.setId(deliver.getId());
        completed.setStatus(Constants.STATUS_COMPLETED);
        when(stepInstanceRepository.findById(deliver.getId().toHexString())).thenReturn(Optional.of(completed));

        assertThat(monitor.checkOverdueSteps()).isEmpty();
        verify(stepProgressService, never()).markOverdue(any(), any());
    }

    private void stored(StepInstance step) {
        when(stepInstanceRepository.findById(step.getId().toHexString())).thenReturn(Optional.of(step));
    }

    private static StepInstance step(String stepCode) {
        StepInstance step = new StepInstance();
        step.setId(new ObjectId());
        step.setStepCode(stepCode);
        step.setStatus(Constants.STATUS_CREATED);
        step.setLateAfter(NOW.minusHours(1));
        return step;
    }
}
