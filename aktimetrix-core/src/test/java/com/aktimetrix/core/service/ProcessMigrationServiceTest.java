package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import com.aktimetrix.core.store.AktimetrixTransactions;
import org.junit.jupiter.api.BeforeEach;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessMigrationServiceTest {

    private static final LocalDateTime BOOKED = LocalDateTime.of(2024, 1, 10, 9, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2024, 1, 10, 12, 30);

    @Mock
    private ProcessInstanceService processInstanceService;
    @Mock
    private ProcessDefinitionService processDefinitionService;
    @Mock
    private StepInstanceService stepInstanceService;
    @Mock
    private StepInstancePublisherService stepInstancePublisherService;
    @Mock
    private ProcessInstancePublisherService processInstancePublisherService;
    @Mock
    private RegistryService registryService;
    @Mock
    private Processor meterProcessor;
    private ProcessMigrationService service;

    @BeforeEach
    void setUp() {
        final AktimetrixTransactions transactions = new AktimetrixTransactions() {
            @Override
            public void run(Runnable work) {
                work.run();
            }

            @Override
            public boolean isAtomic() {
                return true;
            }
        };
        service = new ProcessMigrationService(processInstanceService, processDefinitionService, stepInstanceService,
                stepInstancePublisherService, processInstancePublisherService, registryService, transactions,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    @Test
    void aRunningInstanceIsMovedToTheCurrentRevisionAndReplanned() throws Exception {
        final ProcessDefinition revision1 = definition(1L, step("PICKUP", "PT1H", null), step("SORT", "PT3H", null),
                step("LEGACY", "PT4H", null));
        final ProcessInstance parcel = instance("p-1", revision1);
        final StepInstance pickup = stepInstance("PICKUP", 0, Constants.STATUS_COMPLETED, BOOKED.plusHours(1));
        pickup.setActualAt(BOOKED.plusMinutes(50));
        pickup.setTimeliness(Timeliness.ON_TIME);
        final StepInstance sort = stepInstance("SORT", 1, Constants.STATUS_CREATED, BOOKED.plusHours(3));
        sort.setTimeliness(Timeliness.OVERDUE);
        final StepInstance legacy = stepInstance("LEGACY", 2, Constants.STATUS_CREATED, BOOKED.plusHours(4));
        final ProcessDefinition revision2 = definition(2L, step("PICKUP", "PT1H", null), step("SORT", "PT5H", null),
                step("DELIVER", "PT2H", "SORT"));
        revision2.setPlannedWithin("P1D");
        final ProcessInstance upToDate = instance("p-2", revision2);
        when(processDefinitionService.currentDefinition("T1", "PARCEL")).thenReturn(revision2);
        when(processInstanceService.getRunning("T1", "PARCEL")).thenReturn(List.of(parcel, upToDate));
        when(processInstanceService.getProcessInstance("T1", "p-1")).thenReturn(parcel);
        when(stepInstanceService.getStepInstancesByProcessInstanceId("T1", "p-1"))
                .thenReturn(List.of(pickup, sort, legacy));
        when(registryService.getProcessHandler(Constants.METER_PROCESSOR)).thenReturn(meterProcessor);

        final ProcessMigrationService.Migration migration = service.migrate("T1", "PARCEL");

        assertThat(migration.getMigrated()).containsExactly("p-1");
        assertThat(migration.getUpToDate()).isEqualTo(1);
        assertThat(migration.getFailed()).isEmpty();
        assertThat(migration.getRevision()).isEqualTo(2L);
        assertThat(parcel.getDefinitionRevision()).isEqualTo(2L);
        assertThat(parcel.getDefinition().getSteps()).extracting(StepDefinition::getStepCode)
                .containsExactly("PICKUP", "SORT", "DELIVER");
        assertThat(parcel.getLateAfter()).isEqualTo(BOOKED.plusDays(1));

        assertThat(pickup.getActualAt()).as("what happened is kept").isEqualTo(BOOKED.plusMinutes(50));
        assertThat(pickup.getTimeliness()).isEqualTo(Timeliness.ON_TIME);
        assertThat(sort.getPlannedAt()).isEqualTo(BOOKED.plusHours(5));
        assertThat(sort.getTimeliness()).as("its new deadline is ahead").isNull();
        verify(stepInstancePublisherService).publish(sort, "PLANNED");
        assertThat(legacy.getStatus()).isEqualTo(Constants.STATUS_SKIPPED);
        verify(stepInstancePublisherService).publish(legacy, "SKIPPED");
        verify(stepInstancePublisherService).publish(argThat(step -> "DELIVER".equals(step.getStepCode())
                && step.getSequence() == 2 && step.getPlannedAt() == null), argThat("CREATED"::equals));
        verify(meterProcessor).process(any());
        verify(processInstanceService).saveProcessInstance(parcel);
        verify(processInstancePublisherService).publish(parcel, "MIGRATED");
        verify(processInstancePublisherService, never()).publish(upToDate, "MIGRATED");
    }

    @Test
    void aProcessWithoutADefinitionCannotBeMigrated() {
        assertThatThrownBy(() -> service.migrate("T1", "UNKNOWN")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN");
    }

    private static ProcessDefinition definition(long revision, StepDefinition... steps) {
        final ProcessDefinition definition = new ProcessDefinition("T1", "PARCEL");
        definition.setRevision(revision);
        definition.setSteps(List.of(steps));
        return definition;
    }

    private static StepDefinition step(String code, String within, String after) {
        final StepDefinition step = new StepDefinition();
        step.setStepCode(code);
        step.setPlannedWithin(within);
        step.setPlannedAfter(after);
        return step;
    }

    private static ProcessInstance instance(String id, ProcessDefinition definition) {
        final ProcessInstance instance = new ProcessInstance(definition);
        instance.setId(id);
        instance.setStartedAt(BOOKED);
        return instance;
    }

    private static StepInstance stepInstance(String code, int sequence, String status, LocalDateTime plannedAt) {
        final StepInstance step = new StepInstance("T1", code, "p-1", null, null, "1.0.0", status, BOOKED);
        step.setId("s-" + code);
        step.setSequence(sequence);
        step.setPlannedAt(plannedAt);
        step.setLateAfter(plannedAt);
        return step;
    }
}
