package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.exception.InvalidDefinitionException;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.store.DefinitionStore;
import com.aktimetrix.core.store.StoreDocuments;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Process definitions, and the definition each process instance follows.
 * <p>
 * Definitions are versioned: saving a definition that differs from the stored one increments its {@code revision}.
 * A process instance keeps the definition it started with, its steps resolved, and follows it until it ends; see
 * {@link #definitionOf(ProcessInstance)}.
 */
@Component
@RequiredArgsConstructor
public class ProcessDefinitionService {

    private final DefinitionStore store;

    /**
     * Saves the definition, replacing the one with the same tenant and process code. Its revision is incremented if it
     * differs from the stored one, and kept otherwise, so reloading unchanged definitions creates no new revision.
     *
     * @throws InvalidDefinitionException if the definition is not valid; nothing is saved then
     */
    public ProcessDefinition add(ProcessDefinition definition) {
        final List<String> problems = DefinitionValidator.problems(definition);
        if (!problems.isEmpty()) {
            throw new InvalidDefinitionException(problems);
        }
        final ProcessDefinition existing = store.findProcess(definition.getTenant(), definition.getProcessCode()).orElse(null);
        if (existing == null) {
            definition.setRevision(1L);
        } else {
            definition.setId(existing.getId());
            final long revision = existing.getRevision() == null ? 1L : existing.getRevision();
            definition.setRevision(sameContent(existing, definition) ? revision : revision + 1);
        }
        return store.saveProcess(definition);
    }

    public ProcessDefinition findByCode(String tenant, String processCode) {
        return store.findProcess(tenant, processCode).orElse(null);
    }

    public List<ProcessDefinition> list() {
        return store.findProcesses();
    }

    /**
     * The confirmed process definitions the event code starts, with their steps resolved: each step as the process
     * uses it, the tenant's shared definition overridden by the fields the process sets.
     */
    public List<ProcessDefinition> findStartedBy(String tenant, String eventCode) {
        final List<ProcessDefinition> definitions = store.findConfirmedProcessesStartedBy(tenant, eventCode);
        definitions.forEach(definition -> definition.setSteps(resolveSteps(tenant, definition.getSteps())));
        return definitions;
    }

    /**
     * The current revision of the process definition, with its steps resolved; {@code null} if there is none.
     */
    public ProcessDefinition currentDefinition(String tenant, String processCode) {
        final ProcessDefinition current = findByCode(tenant, processCode);
        if (current != null) {
            current.setSteps(resolveSteps(tenant, current.getSteps()));
        }
        return current;
    }

    /**
     * The definition the process instance follows: the one it started with, or, for an instance started by a version
     * that did not keep it, the current one, with its steps resolved. {@code null} if there is none.
     */
    public ProcessDefinition definitionOf(ProcessInstance instance) {
        if (instance.getDefinition() != null) {
            return instance.getDefinition();
        }
        return currentDefinition(instance.getTenant(), instance.getProcessCode());
    }

    /**
     * The definitions of the steps the process instance follows, by step code.
     */
    public Map<String, StepDefinition> stepDefinitionsOf(ProcessInstance instance) {
        final ProcessDefinition definition = definitionOf(instance);
        if (definition == null || definition.getSteps() == null) {
            return new LinkedHashMap<>();
        }
        return definition.getSteps().stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(StepDefinition::getStepCode, step -> step, (a, b) -> a, LinkedHashMap::new));
    }

    private List<StepDefinition> resolveSteps(String tenant, List<StepDefinition> steps) {
        if (steps == null) {
            return List.of();
        }
        return steps.stream()
                .map(step -> StepDefinitionService.resolve(store.findStep(tenant, step.getStepCode()).orElse(null), step))
                .collect(Collectors.toList());
    }

    private static boolean sameContent(ProcessDefinition stored, ProcessDefinition updated) {
        final ProcessDefinition a = StoreDocuments.copy(stored);
        final ProcessDefinition b = StoreDocuments.copy(updated);
        a.setId(null);
        a.setRevision(null);
        b.setId(null);
        b.setRevision(null);
        return StoreDocuments.toJson(a).equals(StoreDocuments.toJson(b));
    }
}
