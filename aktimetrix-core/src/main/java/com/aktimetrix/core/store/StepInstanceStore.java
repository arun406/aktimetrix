package com.aktimetrix.core.store;

import com.aktimetrix.core.model.StepInstance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Stores step instances, with the same insert and version-check rules as {@link ProcessInstanceStore#save}.
 */
public interface StepInstanceStore {

    StepInstance save(StepInstance step);

    default List<StepInstance> saveAll(List<StepInstance> steps) {
        steps.forEach(this::save);
        return steps;
    }

    Optional<StepInstance> findById(String id);

    /**
     * The steps of the process instance, in {@code sequence} order.
     */
    List<StepInstance> findByProcessInstance(String tenant, String processInstanceId);

    /**
     * Steps of every tenant that are not completed, cancelled or skipped, whose {@code lateAfter} is before
     * {@code now}, and whose timeliness is not {@code OVERDUE} yet.
     */
    List<StepInstance> findOverdue(LocalDateTime now);
}
