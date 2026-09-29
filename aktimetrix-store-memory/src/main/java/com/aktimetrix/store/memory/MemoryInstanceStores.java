package com.aktimetrix.store.memory;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.store.MeasurementInstanceStore;
import com.aktimetrix.core.store.ProcessInstanceStore;
import com.aktimetrix.core.store.StepInstanceStore;
import com.aktimetrix.core.store.StoreDocuments;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Process, step and measurement instances in memory, in insertion order. Each is stored and returned as a copy, as a database would, so
 * a caller's changes are only kept when it saves them, and saves are version-checked.
 */
final class MemoryInstanceStores {

    private static final Set<String> CLOSED_STEP = Set.of(Constants.STATUS_COMPLETED, Constants.STATUS_CANCELLED,
            Constants.STATUS_SKIPPED);

    private MemoryInstanceStores() {
    }

    static String newId() {
        return UUID.randomUUID().toString();
    }

    static final class Processes implements ProcessInstanceStore {
        private final Map<String, ProcessInstance> instances = new LinkedHashMap<>();

        @Override
        public synchronized ProcessInstance save(ProcessInstance instance) {
            if (instance.getId() == null) {
                final boolean duplicate = instances.values().stream().anyMatch(existing ->
                        Objects.equals(existing.getTenant(), instance.getTenant())
                                && Objects.equals(existing.getProcessCode(), instance.getProcessCode())
                                && Objects.equals(existing.getEntityType(), instance.getEntityType())
                                && Objects.equals(existing.getEntityId(), instance.getEntityId()));
                if (duplicate) {
                    throw new DuplicateKeyException("A " + instance.getProcessCode() + " process already exists for "
                            + instance.getEntityType() + " " + instance.getEntityId());
                }
                instance.setId(newId());
                instance.setRevision(0L);
            } else {
                final ProcessInstance stored = instances.get(instance.getId());
                if (stored != null && !Objects.equals(stored.getRevision(), instance.getRevision())) {
                    throw new OptimisticLockingFailureException("Process instance " + instance.getId()
                            + " was changed since it was read");
                }
                instance.setRevision(instance.getRevision() == null ? 0L : instance.getRevision() + 1);
            }
            instances.put(instance.getId(), StoreDocuments.copy(instance));
            return instance;
        }

        @Override
        public synchronized Optional<ProcessInstance> findById(String tenant, String id) {
            return Optional.ofNullable(instances.get(id)).filter(i -> Objects.equals(tenant, i.getTenant()))
                    .map(StoreDocuments::copy);
        }

        @Override
        public synchronized Optional<ProcessInstance> findByEntity(String tenant, String processCode, String entityType,
                                                      String entityId) {
            return find(i -> Objects.equals(tenant, i.getTenant()) && Objects.equals(processCode, i.getProcessCode())
                    && Objects.equals(entityType, i.getEntityType()) && Objects.equals(entityId, i.getEntityId()))
                    .stream().findFirst();
        }

        @Override
        public synchronized List<ProcessInstance> findByEntityId(String tenant, String entityId) {
            return find(i -> Objects.equals(tenant, i.getTenant()) && Objects.equals(entityId, i.getEntityId()));
        }

        @Override
        public synchronized List<ProcessInstance> findNotCancelled(String tenant, String entityType, String entityId) {
            return find(i -> Objects.equals(tenant, i.getTenant()) && Objects.equals(entityType, i.getEntityType())
                    && Objects.equals(entityId, i.getEntityId()) && !Constants.STATUS_CANCELLED.equals(i.getStatus()));
        }

        @Override
        public synchronized List<ProcessInstance> findOverdue(LocalDateTime now) {
            return find(i -> !i.isComplete() && i.getLateAfter() != null && i.getLateAfter().isBefore(now)
                    && i.getTimeliness() != Timeliness.OVERDUE);
        }

        private List<ProcessInstance> find(Predicate<ProcessInstance> filter) {
            return instances.values().stream().filter(filter).map(StoreDocuments::copy).collect(Collectors.toList());
        }
    }

    static final class Steps implements StepInstanceStore {
        private final Map<String, StepInstance> steps = new LinkedHashMap<>();

        @Override
        public synchronized StepInstance save(StepInstance step) {
            if (step.getId() == null) {
                step.setId(newId());
                step.setRevision(0L);
            } else {
                final StepInstance stored = steps.get(step.getId());
                if (stored != null && !Objects.equals(stored.getRevision(), step.getRevision())) {
                    throw new OptimisticLockingFailureException("Step instance " + step.getId()
                            + " was changed since it was read");
                }
                step.setRevision(step.getRevision() == null ? 0L : step.getRevision() + 1);
            }
            steps.put(step.getId(), StoreDocuments.copy(step));
            return step;
        }

        @Override
        public synchronized Optional<StepInstance> findById(String id) {
            return Optional.ofNullable(steps.get(id)).map(StoreDocuments::copy);
        }

        @Override
        public synchronized List<StepInstance> findByProcessInstance(String tenant, String processInstanceId) {
            return steps.values().stream()
                    .filter(s -> Objects.equals(tenant, s.getTenant()) && Objects.equals(processInstanceId, s.getProcessInstanceId()))
                    .sorted(Comparator.comparingInt(StepInstance::getSequence))
                    .map(StoreDocuments::copy)
                    .collect(Collectors.toList());
        }

        @Override
        public synchronized List<StepInstance> findOverdue(LocalDateTime now) {
            return steps.values().stream()
                    .filter(s -> !CLOSED_STEP.contains(s.getStatus()) && s.getLateAfter() != null
                            && s.getLateAfter().isBefore(now) && s.getTimeliness() != Timeliness.OVERDUE)
                    .map(StoreDocuments::copy)
                    .collect(Collectors.toList());
        }
    }

    static final class Measurements implements MeasurementInstanceStore {
        private final Map<String, MeasurementInstance> measurements = new LinkedHashMap<>();

        @Override
        public synchronized MeasurementInstance save(MeasurementInstance measurement) {
            if (measurement.getId() == null) {
                measurement.setId(newId());
            }
            measurements.put(measurement.getId(), StoreDocuments.copy(measurement));
            return measurement;
        }

        @Override
        public synchronized List<MeasurementInstance> findByProcessInstance(String tenant, String processInstanceId) {
            return find(m -> Objects.equals(tenant, m.getTenant()) && Objects.equals(processInstanceId, m.getProcessInstanceId()));
        }

        @Override
        public synchronized List<MeasurementInstance> find(String tenant, String processInstanceId, String stepInstanceId,
                                              String code, String type) {
            return find(m -> Objects.equals(tenant, m.getTenant()) && Objects.equals(processInstanceId, m.getProcessInstanceId())
                    && Objects.equals(stepInstanceId, m.getStepInstanceId()) && Objects.equals(code, m.getCode())
                    && Objects.equals(type, m.getType()));
        }

        private List<MeasurementInstance> find(Predicate<MeasurementInstance> filter) {
            return measurements.values().stream().filter(filter).map(StoreDocuments::copy).collect(Collectors.toList());
        }
    }
}
