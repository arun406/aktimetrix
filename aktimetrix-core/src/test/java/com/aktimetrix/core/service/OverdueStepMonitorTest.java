package com.aktimetrix.core.service;

import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.repository.StepInstanceRepository;
import com.aktimetrix.core.storage.AktimetrixTransactions;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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

        assertThat(monitor.checkOverdueSteps()).containsExactly(deliver);
        verify(stepProgressService).markOverdue(deliver, NOW);
    }

    @Test
    void skipsAStepThatChangedSinceItWasReadAndCarriesOn() {
        StepInstance ship = step("SHIP");
        StepInstance deliver = step("DELIVER");
        when(stepInstanceRepository.findOverdue(NOW)).thenReturn(List.of(ship, deliver));
        // e.g. another instance marked SHIP first, or its event just completed it
        doThrow(new OptimisticLockingFailureException("stale")).when(stepProgressService).markOverdue(ship, NOW);

        assertThat(monitor.checkOverdueSteps()).containsExactly(deliver);
        verify(stepProgressService).markOverdue(deliver, NOW);
    }

    private static StepInstance step(String stepCode) {
        StepInstance step = new StepInstance();
        step.setStepCode(stepCode);
        return step;
    }
}
