package com.aktimetrix.core.service;

import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.repository.StepInstanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverdueStepMonitorTest {

    @Mock
    private StepInstanceRepository stepInstanceRepository;
    @Mock
    private StepProgressService stepProgressService;

    @Test
    void marksStepsPastTheirDeadlineOverdue() {
        Clock clock = Clock.fixed(Instant.parse("2022-05-23T12:00:00Z"), ZoneOffset.UTC);
        LocalDateTime now = LocalDateTime.of(2022, 5, 23, 12, 0);
        StepInstance deliver = new StepInstance();
        deliver.setStepCode("DELIVER");
        when(stepInstanceRepository.findOverdue(now)).thenReturn(List.of(deliver));

        List<StepInstance> overdue = new OverdueStepMonitor(stepInstanceRepository, stepProgressService, clock)
                .checkOverdueSteps();

        assertThat(overdue).containsExactly(deliver);
        verify(stepProgressService).markOverdue(deliver, now);
    }
}
