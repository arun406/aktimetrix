package com.aktimetrix.core.service;

import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.repository.ProcessInstanceRepository;
import com.aktimetrix.core.storage.AktimetrixTransactions;
import org.bson.types.ObjectId;
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
class OverdueProcessMonitorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2022-05-23T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2022, 5, 23, 12, 0);

    @Mock
    private ProcessInstanceRepository processInstanceRepository;
    @Mock
    private StepProgressService stepProgressService;
    @Mock
    private AktimetrixTransactions transactions;

    @Test
    void marksProcessesPastTheirDeadlineOverdueAndSkipsConflicts() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactions).run(any());
        ProcessInstance first = new ProcessInstance();
        first.setId(new ObjectId());
        ProcessInstance second = new ProcessInstance();
        second.setId(new ObjectId());
        when(processInstanceRepository.findOverdue(NOW)).thenReturn(List.of(first, second));
        doThrow(new OptimisticLockingFailureException("stale")).when(stepProgressService).markProcessOverdue(first);

        List<ProcessInstance> overdue = new OverdueProcessMonitor(processInstanceRepository, stepProgressService, CLOCK,
                transactions).checkOverdueProcesses();

        assertThat(overdue).containsExactly(second);
        verify(stepProgressService).markProcessOverdue(second);
    }
}
