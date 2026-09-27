package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Timeliness;
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
    private StepInstanceService stepInstanceService;
    @Mock
    private StepInstancePublisherService stepInstancePublisherService;

    @Test
    void marksAndPublishesStepsPastTheirPlan() {
        Clock clock = Clock.fixed(Instant.parse("2022-05-23T12:00:00Z"), ZoneOffset.UTC);
        StepInstance deliver = new StepInstance();
        deliver.setStepCode("DELIVER");
        deliver.setPlannedAt(LocalDateTime.of(2022, 5, 23, 9, 46));
        when(stepInstanceRepository.findOverdue(LocalDateTime.of(2022, 5, 23, 12, 0))).thenReturn(List.of(deliver));

        List<StepInstance> overdue = new OverdueStepMonitor(stepInstanceRepository, stepInstanceService,
                stepInstancePublisherService, clock).checkOverdueSteps();

        assertThat(overdue).containsExactly(deliver);
        assertThat(deliver.getTimeliness()).isEqualTo(Timeliness.OVERDUE);
        verify(stepInstanceService).save(deliver);
        verify(stepInstancePublisherService).publish(deliver, "OVERDUE");
    }
}
