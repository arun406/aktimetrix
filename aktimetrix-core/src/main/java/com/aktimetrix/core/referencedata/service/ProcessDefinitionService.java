package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.repository.ProcessDefinitionRepository;
import com.aktimetrix.core.referencedata.repository.StepDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProcessDefinitionService {

    private final ProcessDefinitionRepository repository;
    private final StepDefinitionRepository stepDefinitionRepository;

    /**
     * Saves the process definition, replacing an existing one with the same tenant and process code.
     */
    public ProcessDefinition add(ProcessDefinition definition) {
        repository.findByTenantAndProcessCode(definition.getTenant(), definition.getProcessCode()).stream()
                .findFirst()
                .ifPresent(existing -> definition.setId(existing.getId()));
        this.repository.save(definition);
        return definition;
    }

    /**
     * Returns all process definitions as stored.
     */
    public List<ProcessDefinition> list() {
        return this.repository.findAll();
    }

    /**
     * Returns the confirmed process definitions of the tenant that the event code starts, with each step resolved
     * to the tenant's step definition, in the order the process lists them.
     */
    public List<ProcessDefinition> findStartedBy(String tenant, String eventCode) {
        final List<ProcessDefinition> definitions = repository.findConfirmedStartedBy(tenant, eventCode);
        definitions.forEach(definition -> definition.setSteps(resolveSteps(tenant, definition.getSteps())));
        return definitions;
    }

    private List<StepDefinition> resolveSteps(String tenant, List<StepDefinition> steps) {
        if (steps == null) {
            return List.of();
        }
        return steps.stream()
                .map(step -> {
                    StepDefinition resolved = stepDefinitionRepository.findByStepCode(tenant, step.getStepCode());
                    return resolved != null ? resolved : step;
                })
                .collect(Collectors.toList());
    }
}
