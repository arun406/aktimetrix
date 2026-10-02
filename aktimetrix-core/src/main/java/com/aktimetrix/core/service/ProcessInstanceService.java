package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.store.ProcessInstanceStore;
import com.aktimetrix.core.store.StepInstanceStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessInstanceService.class);

    private final ProcessInstanceStore store;
    private final StepInstanceStore stepInstanceStore;
    private final DeadlineAlarms alarms;

    /**
     * Saves the process instance: inserts it, assigning its id, or updates it with a version check. Its deadline alarm
     * is set, moved or cancelled with it.
     *
     * @param processInstance process instance to be saved
     * @return saved process instance
     */
    public ProcessInstance saveProcessInstance(ProcessInstance processInstance) {
        if (processInstance.getId() != null) {
            alarms.reconcile(processInstance);
        }
        store.save(processInstance);
        if (DeadlineAlarms.needsAlarm(processInstance)) {
            // a new process with a deadline: its alarm needs its id
            alarms.reconcile(processInstance);
            store.save(processInstance);
        }
        logger.info("Process Instance Id :" + processInstance.getId());
        return processInstance;
    }

    /**
     * Returns the latest run of the process for the entity, whatever its status, or {@code null}.
     */
    public ProcessInstance getProcessInstance(String tenant, String processCode, String entityType, String entityId) {
        // not filtered by status, so a completed process is not re-created when its start event is replayed
        return store.findByEntity(tenant, processCode, entityType, entityId).orElse(null);
    }

    /**
     * Every run of the process for the entity, first to last.
     */
    public List<ProcessInstance> getRuns(String tenant, String processCode, String entityType, String entityId) {
        return store.findByEntityId(tenant, entityId).stream()
                .filter(instance -> processCode.equals(instance.getProcessCode())
                        && Objects.equals(entityType, instance.getEntityType()))
                .sorted(Comparator.comparingInt(ProcessInstance::getRun))
                .collect(Collectors.toList());
    }

    /**
     * The latest run of each process of the business entity, unless it was cancelled: the runs that business events
     * still apply to. An earlier run has ended for good, and is left alone.
     */
    public List<ProcessInstance> getCurrentRuns(String tenant, String entityType, String entityId) {
        final Map<String, ProcessInstance> latest = new LinkedHashMap<>();
        for (ProcessInstance instance : store.findByEntityId(tenant, entityId)) {
            if (!Objects.equals(entityType, instance.getEntityType())) {
                continue;
            }
            latest.merge(instance.getProcessCode(), instance, (a, b) -> a.getRun() >= b.getRun() ? a : b);
        }
        return latest.values().stream()
                .filter(instance -> !Constants.STATUS_CANCELLED.equals(instance.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Returns every process instance of the business entity, with its step instances.
     *
     * @param entityType entity type to match, or {@code null} for any
     */
    public List<ProcessInstance> getProcessInstancesWithSteps(String tenant, String entityType, String entityId) {
        final List<ProcessInstance> instances = store.findByEntityId(tenant, entityId).stream()
                .filter(instance -> entityType == null || entityType.equals(instance.getEntityType()))
                .collect(Collectors.toList());
        instances.forEach(instance -> instance.setSteps(stepInstanceStore.findByProcessInstance(tenant, instance.getId())));
        return instances;
    }

    /**
     * Returns the process instances of the given business entity that are not cancelled.
     */
    public List<ProcessInstance> getNotCancelledProcessInstances(String tenant, String entityType, String entityId) {
        return store.findNotCancelled(tenant, entityType, entityId);
    }

    /**
     * The instances of the process that are still running, for every entity.
     */
    public List<ProcessInstance> getRunning(String tenant, String processCode) {
        return store.findRunning(tenant, processCode);
    }

    /**
     * Returns the process instance by id, or {@code null}.
     */
    public ProcessInstance getProcessInstance(String tenant, String processInstanceId) {
        return store.findById(tenant, processInstanceId).orElse(null);
    }
}
