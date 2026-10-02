package com.aktimetrix.store;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.Alarm;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.outbox.OutboxMessage;
import com.aktimetrix.core.referencedata.model.MeasurementDefinition;
import com.aktimetrix.core.referencedata.model.MeasurementTypeDefinition;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.store.AktimetrixTransactions;
import com.aktimetrix.core.store.AlarmStore;
import com.aktimetrix.core.store.DefinitionStore;
import com.aktimetrix.core.store.MeasurementInstanceStore;
import com.aktimetrix.core.store.OutboxStore;
import com.aktimetrix.core.store.ProcessedEventStore;
import com.aktimetrix.core.store.ProcessInstanceStore;
import com.aktimetrix.core.store.StepInstanceStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * What every store module must do, whatever its database: the semantics the Aktimetrix services rely on. A new store
 * module passes when a subclass that starts it passes.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class StoreContractTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2024, 3, 1, 12, 0);

    private ConfigurableApplicationContext context;
    protected ProcessInstanceStore processes;
    protected StepInstanceStore steps;
    protected MeasurementInstanceStore measurements;
    protected DefinitionStore definitions;
    protected OutboxStore outbox;
    protected AlarmStore alarms;
    protected ProcessedEventStore processedEvents;
    protected AktimetrixTransactions transactions;

    /**
     * Starts an application context with the store module under test.
     */
    protected abstract ConfigurableApplicationContext start();

    @BeforeAll
    void startStore() {
        context = start();
        processes = context.getBean(ProcessInstanceStore.class);
        steps = context.getBean(StepInstanceStore.class);
        measurements = context.getBean(MeasurementInstanceStore.class);
        definitions = context.getBean(DefinitionStore.class);
        outbox = context.getBean(OutboxStore.class);
        alarms = context.getBean(AlarmStore.class);
        processedEvents = context.getBean(ProcessedEventStore.class);
        transactions = context.getBean(AktimetrixTransactions.class);
    }

    protected ConfigurableApplicationContext context() {
        return context;
    }

    @AfterAll
    void stopStore() {
        context.close();
    }

    private static String unique() {
        return UUID.randomUUID().toString();
    }

    private static ProcessInstance process(String tenant, String entityId) {
        final ProcessDefinition definition = new ProcessDefinition(tenant, "ORDER_DELIVERY");
        definition.setEntityType("com.ecom.order");
        definition.setRevision(3L);
        definition.setPlannedWithin("P1D");
        final StepDefinition travel = new StepDefinition();
        travel.setStepCode("TRAVEL");
        travel.setPlannedWithin("PT3H");
        final MeasurementDefinition distance = new MeasurementDefinition();
        distance.setMeasurementCode("DISTANCE");
        distance.setValue("5");
        travel.setMeasurements(List.of(distance));
        definition.setSteps(List.of(travel));
        final ProcessInstance instance = new ProcessInstance(definition);
        instance.setEntityId(entityId);
        instance.setStartedAt(NOW.minusHours(3));
        instance.setMetadata(Map.of("priority", true, "orderId", entityId));
        return instance;
    }

    private static StepInstance step(String tenant, String processInstanceId, String code, int sequence) {
        final StepInstance step = new StepInstance(tenant, code, processInstanceId, null, null, "1.0.0",
                Constants.STATUS_CREATED, NOW);
        step.setSequence(sequence);
        return step;
    }

    // process instances

    @Test
    void anInsertedProcessGetsAnIdAndIsFoundByIdAndByEntity() {
        final String tenant = unique();
        final ProcessInstance saved = processes.save(process(tenant, "1234"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRevision()).isZero();
        assertThat(processes.findById(tenant, saved.getId())).get().satisfies(found -> {
            assertThat(found.getEntityId()).isEqualTo("1234");
            assertThat(found.getMetadata()).containsEntry("orderId", "1234").containsEntry("priority", true);
            assertThat(found.getStartedAt()).isEqualTo(NOW.minusHours(3));
        });
        assertThat(processes.findById(unique(), saved.getId())).as("another tenant's").isEmpty();
        assertThat(processes.findByEntity(tenant, "ORDER_DELIVERY", "com.ecom.order", "1234")).isPresent();
        assertThat(processes.findByEntityId(tenant, "1234")).hasSize(1);
    }

    @Test
    void aProcessKeepsTheDefinitionItStartedWith() {
        final String tenant = unique();
        final ProcessInstance saved = processes.save(process(tenant, "1234"));

        final ProcessInstance found = processes.findById(tenant, saved.getId()).orElseThrow();
        assertThat(found.getDefinitionRevision()).isEqualTo(3L);
        assertThat(found.getDefinition().getPlannedWithin()).isEqualTo("P1D");
        assertThat(found.getDefinition().getSteps()).singleElement().satisfies(step -> {
            assertThat(step.getStepCode()).isEqualTo("TRAVEL");
            assertThat(step.getMeasurements()).singleElement()
                    .satisfies(m -> assertThat(m.getValue()).isEqualTo("5"));
        });
    }

    @Test
    void aSaveBasedOnAStaleCopyFails() {
        final String tenant = unique();
        final ProcessInstance saved = processes.save(process(tenant, "1234"));
        final ProcessInstance first = processes.findById(tenant, saved.getId()).orElseThrow();
        final ProcessInstance second = processes.findById(tenant, saved.getId()).orElseThrow();

        first.setStatus(Constants.STATUS_COMPLETED);
        processes.save(first);
        assertThat(first.getRevision()).isEqualTo(1L);

        second.setStatus(Constants.STATUS_CANCELLED);
        assertThatThrownBy(() -> processes.save(second)).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(processes.findById(tenant, saved.getId()).orElseThrow().getStatus())
                .isEqualTo(Constants.STATUS_COMPLETED);
    }

    @Test
    void thereIsAtMostOneInstanceOfAProcessPerEntityAndRun() {
        final String tenant = unique();
        final ProcessInstance first = processes.save(process(tenant, "1234"));

        assertThatThrownBy(() -> processes.save(process(tenant, "1234"))).isInstanceOf(DuplicateKeyException.class);

        final ProcessInstance second = process(tenant, "1234");
        second.setRun(2);
        processes.save(second);
        assertThat(processes.findByEntity(tenant, "ORDER_DELIVERY", "com.ecom.order", "1234")).get()
                .as("the latest run").satisfies(found -> {
                    assertThat(found.getId()).isEqualTo(second.getId());
                    assertThat(found.getRun()).isEqualTo(2);
                });
        assertThat(processes.findById(tenant, first.getId())).get().extracting(ProcessInstance::getRun).isEqualTo(1);
        assertThat(processes.findByEntityId(tenant, "1234")).hasSize(2);
    }

    @Test
    void anEventIsProcessedOncePerTenantAndForgottenAfterItsRetention() {
        final String tenant = unique();
        final Instant at = Instant.parse("2024-01-10T09:00:00Z");
        assertThat(processedEvents.isProcessed(tenant, "e1")).isFalse();

        processedEvents.markProcessed(tenant, "e1", at);

        assertThat(processedEvents.isProcessed(tenant, "e1")).isTrue();
        assertThat(processedEvents.isProcessed(unique(), "e1")).as("another tenant's").isFalse();
        assertThatThrownBy(() -> processedEvents.markProcessed(tenant, "e1", at))
                .isInstanceOf(DuplicateKeyException.class);
        processedEvents.markProcessed(tenant, "e2", at.plusSeconds(3600));

        assertThat(processedEvents.deleteProcessedBefore(at.plusSeconds(60))).isGreaterThanOrEqualTo(1);
        assertThat(processedEvents.isProcessed(tenant, "e1")).isFalse();
        assertThat(processedEvents.isProcessed(tenant, "e2")).isTrue();
    }

    @Test
    void cancelledProcessesAreLeftOut() {
        final String tenant = unique();
        final ProcessInstance running = processes.save(process(tenant, "1234"));
        final ProcessInstance other = process(tenant, "1234");
        other.setProcessCode("RETURNS");
        other.setStatus(Constants.STATUS_CANCELLED);
        processes.save(other);

        assertThat(processes.findNotCancelled(tenant, "com.ecom.order", "1234"))
                .extracting(ProcessInstance::getId).containsExactly(running.getId());
    }

    @Test
    void runningInstancesOfAProcessAreFoundForEveryEntity() {
        final String tenant = unique();
        final ProcessInstance first = processes.save(process(tenant, "1"));
        final ProcessInstance second = processes.save(process(tenant, "2"));
        final ProcessInstance done = process(tenant, "3");
        done.setComplete(true);
        processes.save(done);
        final ProcessInstance other = process(tenant, "4");
        other.setProcessCode("RETURNS");
        processes.save(other);
        processes.save(process(unique(), "1"));

        assertThat(processes.findRunning(tenant, "ORDER_DELIVERY")).extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    void overdueProcessesAreThoseRunningPastTheirDeadline() {
        final String tenant = unique();
        final ProcessInstance late = process(tenant, "late");
        late.setLateAfter(NOW.minusMinutes(1));
        processes.save(late);
        final ProcessInstance notYet = process(tenant, "not-yet");
        notYet.setLateAfter(NOW.plusMinutes(1));
        processes.save(notYet);
        final ProcessInstance done = process(tenant, "done");
        done.setLateAfter(NOW.minusMinutes(1));
        done.setComplete(true);
        processes.save(done);
        final ProcessInstance marked = process(tenant, "marked");
        marked.setLateAfter(NOW.minusMinutes(1));
        marked.setTimeliness(Timeliness.OVERDUE);
        processes.save(marked);

        assertThat(processes.findOverdue(NOW)).filteredOn(p -> tenant.equals(p.getTenant()))
                .extracting(ProcessInstance::getEntityId).containsExactly("late");
    }

    // step instances

    @Test
    void stepsAreReturnedInSequenceAndSavesAreVersionChecked() {
        final String tenant = unique();
        final String processId = unique();
        steps.saveAll(new ArrayList<>(List.of(step(tenant, processId, "SHIP", 1), step(tenant, processId, "PLACE", 0))));

        final List<StepInstance> found = steps.findByProcessInstance(tenant, processId);
        assertThat(found).extracting(StepInstance::getStepCode).containsExactly("PLACE", "SHIP");

        final StepInstance place = found.get(0);
        final StepInstance stale = steps.findById(place.getId()).orElseThrow();
        place.setStatus(Constants.STATUS_COMPLETED);
        place.setActualAt(NOW);
        steps.save(place);
        assertThat(steps.findById(place.getId()).orElseThrow().getActualAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> steps.save(stale)).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void overdueStepsAreOpenStepsPastTheirDeadline() {
        final String tenant = unique();
        final String processId = unique();
        final StepInstance late = step(tenant, processId, "LATE", 0);
        late.setLateAfter(NOW.minusMinutes(1));
        final StepInstance completed = step(tenant, processId, "COMPLETED", 1);
        completed.setLateAfter(NOW.minusMinutes(1));
        completed.setStatus(Constants.STATUS_COMPLETED);
        final StepInstance skipped = step(tenant, processId, "SKIPPED", 2);
        skipped.setLateAfter(NOW.minusMinutes(1));
        skipped.setStatus(Constants.STATUS_SKIPPED);
        final StepInstance marked = step(tenant, processId, "MARKED", 3);
        marked.setLateAfter(NOW.minusMinutes(1));
        marked.setTimeliness(Timeliness.OVERDUE);
        final StepInstance notYet = step(tenant, processId, "NOT_YET", 4);
        notYet.setLateAfter(NOW.plusMinutes(1));
        steps.saveAll(new ArrayList<>(List.of(late, completed, skipped, marked, notYet)));

        assertThat(steps.findOverdue(NOW)).filteredOn(s -> tenant.equals(s.getTenant()))
                .extracting(StepInstance::getStepCode).containsExactly("LATE");
    }

    // measurement instances

    @Test
    void measurementsAreFoundByOwnerCodeAndType() {
        final String tenant = unique();
        final String processId = unique();
        final String stepId = unique();
        final MeasurementInstance planned = new MeasurementInstance(tenant, "DISTANCE", "5", "KM", processId, stepId,
                "TRAVEL", Constants.PLAN_MEASUREMENT_TYPE, null, ZonedDateTime.of(NOW, ZoneOffset.UTC));
        final MeasurementInstance actual = new MeasurementInstance(tenant, "DISTANCE", "12", "KM", processId, stepId,
                "TRAVEL", Constants.ACTUAL_MEASUREMENT_TYPE, null, ZonedDateTime.of(NOW, ZoneOffset.UTC));
        actual.setPlannedValue("5");
        actual.setDeviation("7");
        final MeasurementInstance cost = new MeasurementInstance(tenant, "COST", "8", "EUR", processId, null, null,
                Constants.PLAN_MEASUREMENT_TYPE, null, ZonedDateTime.of(NOW, ZoneOffset.UTC));
        measurements.saveAll(new ArrayList<>(List.of(planned, actual, cost)));

        assertThat(planned.getId()).isNotNull();
        assertThat(measurements.find(tenant, processId, stepId, "DISTANCE", Constants.PLAN_MEASUREMENT_TYPE))
                .singleElement().satisfies(m -> assertThat(m.getValue()).isEqualTo("5"));
        assertThat(measurements.find(tenant, processId, null, "COST", Constants.PLAN_MEASUREMENT_TYPE))
                .as("a process-level measurement").singleElement().satisfies(m -> assertThat(m.getValue()).isEqualTo("8"));
        assertThat(measurements.find(tenant, processId, null, "DISTANCE", Constants.PLAN_MEASUREMENT_TYPE)).isEmpty();
        assertThat(measurements.findByProcessInstance(tenant, processId)).hasSize(3)
                .filteredOn(m -> "12".equals(m.getValue())).singleElement()
                .satisfies(m -> assertThat(m.getDeviation()).isEqualTo("7"));
    }

    // definitions

    @Test
    void savingADefinitionReplacesTheOneWithTheSameTenantAndCode() {
        final String tenant = unique();
        final ProcessDefinition first = new ProcessDefinition(tenant, "ORDER_DELIVERY");
        first.setStatus("CONFIRMED");
        first.setStartEventCodes(List.of("ORDER_CREATED_EVENT"));
        definitions.saveProcess(first);
        final ProcessDefinition second = new ProcessDefinition(tenant, "ORDER_DELIVERY");
        second.setStatus("CONFIRMED");
        second.setStartEventCodes(List.of("ORDER_BOOKED_EVENT"));
        second.setRevision(2L);
        definitions.saveProcess(second);

        assertThat(definitions.findProcess(tenant, "ORDER_DELIVERY")).get().satisfies(found -> {
            assertThat(found.getId()).isEqualTo(first.getId());
            assertThat(found.getRevision()).isEqualTo(2L);
        });
        assertThat(definitions.findProcesses()).filteredOn(d -> tenant.equals(d.getTenant())).hasSize(1);
    }

    @Test
    void confirmedProcessesAreFoundByTheirStartEvents() {
        final String tenant = unique();
        final ProcessDefinition delivery = new ProcessDefinition(tenant, "ORDER_DELIVERY");
        delivery.setStatus("CONFIRMED");
        delivery.setStartEventCodes(List.of("ORDER_CREATED_EVENT", "ORDER_IMPORTED_EVENT"));
        definitions.saveProcess(delivery);
        final ProcessDefinition draft = new ProcessDefinition(tenant, "RETURNS");
        draft.setStatus("DRAFT");
        draft.setStartEventCodes(List.of("ORDER_CREATED_EVENT"));
        definitions.saveProcess(draft);
        final ProcessDefinition elsewhere = new ProcessDefinition(unique(), "ORDER_DELIVERY");
        elsewhere.setStatus("CONFIRMED");
        elsewhere.setStartEventCodes(List.of("ORDER_CREATED_EVENT"));
        definitions.saveProcess(elsewhere);

        assertThat(definitions.findConfirmedProcessesStartedBy(tenant, "ORDER_IMPORTED_EVENT"))
                .extracting(ProcessDefinition::getProcessCode).containsExactly("ORDER_DELIVERY");
        assertThat(definitions.findConfirmedProcessesStartedBy(tenant, "ORDER_CREATED_EVENT"))
                .extracting(ProcessDefinition::getProcessCode).containsExactly("ORDER_DELIVERY");
    }

    @Test
    void stepDefinitionsAndMeasurementTypesAreStored() {
        final String tenant = unique();
        final StepDefinition travel = new StepDefinition();
        travel.setTenant(tenant);
        travel.setStepCode("TRAVEL");
        travel.setPlannedWithin("PT3H");
        definitions.saveStep(travel);
        final StepDefinition changed = new StepDefinition();
        changed.setTenant(tenant);
        changed.setStepCode("TRAVEL");
        changed.setPlannedWithin("PT2H");
        definitions.saveStep(changed);
        final MeasurementTypeDefinition distance = new MeasurementTypeDefinition();
        distance.setTenant(tenant);
        distance.setCode("DISTANCE");
        distance.setUnitCode("KM");
        definitions.saveMeasurementType(distance);

        assertThat(definitions.findStep(tenant, "TRAVEL")).get()
                .satisfies(found -> assertThat(found.getPlannedWithin()).isEqualTo("PT2H"));
        assertThat(definitions.findSteps()).filteredOn(s -> tenant.equals(s.getTenant())).hasSize(1);
        assertThat(definitions.findMeasurementTypes()).filteredOn(t -> tenant.equals(t.getTenant()))
                .singleElement().satisfies(t -> assertThat(t.getUnitCode()).isEqualTo("KM"));
    }

    // outbox

    @Test
    void theOutboxHandsOutEachMessageOnceWhileItsLeaseHolds() {
        final Instant now = Instant.parse("2030-01-01T09:00:00Z");
        final OutboxMessage older = new OutboxMessage("step-instance-out-0", "key-1", "{\"n\":1}", now.minusSeconds(20));
        final OutboxMessage newer = new OutboxMessage("step-instance-out-0", "key-2", "{\"n\":2}", now.minusSeconds(10));
        outbox.add(newer);
        outbox.add(older);
        final long pending = outbox.countPending();

        final OutboxMessage claimed = outbox.claimNext(now, now.plusSeconds(30)).orElseThrow();
        assertThat(claimed.getId()).isEqualTo(older.getId());
        assertThat(claimed.getPayload()).isEqualTo("{\"n\":1}");
        assertThat(claimed.getMessageKey()).isEqualTo("key-1");
        assertThat(claimed.getAttempts()).isEqualTo(1);

        assertThat(outbox.claimNext(now, now.plusSeconds(30)).orElseThrow().getId())
                .as("the next one, while the first is leased").isEqualTo(newer.getId());
        assertThat(outbox.claimNext(now, now.plusSeconds(30))).as("both leased").isEmpty();

        final OutboxMessage retried = outbox.claimNext(now.plusSeconds(31), now.plusSeconds(61)).orElseThrow();
        assertThat(retried.getId()).as("after its lease expired").isEqualTo(older.getId());
        assertThat(retried.getAttempts()).isEqualTo(2);

        outbox.markSent(older.getId(), now.plusSeconds(32));
        outbox.markSent(newer.getId(), now.plusSeconds(32));
        assertThat(outbox.countPending()).isEqualTo(pending - 2);
        assertThat(outbox.claimNext(now.plusSeconds(3600), now.plusSeconds(7200))).isEmpty();
        assertThat(outbox.deleteSentBefore(now.plusSeconds(33))).isGreaterThanOrEqualTo(2);
    }

    // alarms

    @Test
    void dueAlarmsAreClaimedEarliestFirstAndOnceWhileTheirLeaseHolds() {
        final LocalDateTime base = LocalDateTime.of(1990, 1, 1, 0, 0);
        final LocalDateTime now = base.plusMinutes(5);
        final Instant at = Instant.parse("2024-03-01T12:00:00Z");
        alarms.claimDue(now, at, at.plusSeconds(3600), 10_000); // leases any alarm left by another test
        final String tenant = unique();
        final Alarm later = Alarm.of(Alarm.STEP, tenant, unique(), "p1", base.plusMinutes(2));
        final Alarm earlier = Alarm.of(Alarm.PROCESS, tenant, unique(), "p2", base.plusMinutes(1));
        final Alarm notYet = Alarm.of(Alarm.STEP, tenant, unique(), "p1", base.plusMinutes(10));
        final long pending = alarms.countPending();
        alarms.schedule(later);
        alarms.schedule(earlier);
        alarms.schedule(notYet);
        assertThat(alarms.countPending()).isEqualTo(pending + 3);

        final List<Alarm> first = alarms.claimDue(now, at, at.plusSeconds(30), 1);
        assertThat(ids(first)).containsExactly(earlier.getId());
        assertThat(first.get(0).getDueAt()).isEqualTo(earlier.getDueAt());
        assertThat(first.get(0).getKind()).isEqualTo(Alarm.PROCESS);
        assertThat(first.get(0).getTargetId()).isEqualTo(earlier.getTargetId());
        assertThat(ids(alarms.claimDue(now, at, at.plusSeconds(30), 10))).containsExactly(later.getId());
        assertThat(alarms.claimDue(now, at, at.plusSeconds(30), 10)).as("both leased, one not due").isEmpty();

        assertThat(ids(alarms.claimDue(now, at.plusSeconds(31), at.plusSeconds(61), 10)))
                .as("claimed again once the lease expired").containsExactly(earlier.getId(), later.getId());

        // moving an alarm releases its claim; cancelling one removes it
        alarms.schedule(Alarm.of(Alarm.STEP, tenant, later.getTargetId(), "p1", base.plusMinutes(3)));
        alarms.schedule(Alarm.of(Alarm.STEP, tenant, later.getTargetId(), "p1", base.plusMinutes(4)));
        alarms.cancel(earlier.getId());
        alarms.cancel(Alarm.idOf(Alarm.STEP, unique()));
        assertThat(alarms.countPending()).isEqualTo(pending + 2);
        final List<Alarm> moved = alarms.claimDue(now, at.plusSeconds(32), at.plusSeconds(62), 10);
        assertThat(ids(moved)).containsExactly(later.getId());
        assertThat(moved.get(0).getDueAt()).isEqualTo(base.plusMinutes(4));

        alarms.cancel(later.getId());
        assertThat(ids(alarms.claimDue(base.plusMinutes(11), at.plusSeconds(63), at.plusSeconds(93), 10)))
                .as("due once its time has passed").containsExactly(notYet.getId());
        alarms.cancel(notYet.getId());
        assertThat(alarms.countPending()).isEqualTo(pending);
    }

    private static List<String> ids(List<Alarm> claimed) {
        return claimed.stream().map(Alarm::getId).collect(Collectors.toList());
    }

    // transactions

    @Test
    void anAtomicUnitOfWorkThatFailsKeepsNothing() {
        assumeTrue(transactions.isAtomic(), "this store's units of work are not atomic");
        final String tenant = unique();

        assertThatThrownBy(() -> transactions.run(() -> {
            processes.save(process(tenant, "1234"));
            outbox.add(new OutboxMessage("process-instance-out-0", "k", "{}", Instant.now()));
            throw new IllegalStateException("fails half-way");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(processes.findByEntityId(tenant, "1234")).isEmpty();
    }

    @Test
    void aUnitOfWorkThatSucceedsKeepsEverything() {
        final String tenant = unique();
        transactions.run(() -> processes.save(process(tenant, "1234")));

        assertThat(processes.findByEntityId(tenant, "1234")).hasSize(1);
    }
}
