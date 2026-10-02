package com.aktimetrix.core.store;

import com.aktimetrix.core.model.ProcessInstance;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Stores process instances.
 * <p>
 * {@link #save} inserts an instance without an id, assigning one, and otherwise updates it with a version check: it
 * increments {@code revision}, and throws {@link org.springframework.dao.OptimisticLockingFailureException} when the
 * stored revision is not the one the instance was read with. At most one instance may exist per tenant, process code,
 * entity type, entity id and run: inserting a second throws {@link org.springframework.dao.DuplicateKeyException}.
 */
public interface ProcessInstanceStore {

    ProcessInstance save(ProcessInstance instance);

    Optional<ProcessInstance> findById(String tenant, String id);

    /**
     * The latest run of the process for the entity, whatever its status.
     */
    Optional<ProcessInstance> findByEntity(String tenant, String processCode, String entityType, String entityId);

    /**
     * Every instance of any process for the entity id, every run, whatever its entity type and status.
     */
    List<ProcessInstance> findByEntityId(String tenant, String entityId);

    /**
     * The instances for the entity that are not cancelled: running and completed ones.
     */
    List<ProcessInstance> findNotCancelled(String tenant, String entityType, String entityId);

    /**
     * The instances of the process that are not complete, every entity and run.
     */
    List<ProcessInstance> findRunning(String tenant, String processCode);

    /**
     * Instances of every tenant that are not complete, whose {@code lateAfter} is before {@code now}, and whose
     * timeliness is not {@code OVERDUE} yet.
     */
    List<ProcessInstance> findOverdue(Instant now);
}
