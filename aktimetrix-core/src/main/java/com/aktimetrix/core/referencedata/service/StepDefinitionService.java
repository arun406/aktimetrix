package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.store.DefinitionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StepDefinitionService {

    private final DefinitionStore store;

    /**
     * Saves the definition, replacing the one with the same tenant and step code.
     */
    public StepDefinition add(StepDefinition stepDefinition) {
        store.findStep(stepDefinition.getTenant(), stepDefinition.getStepCode())
                .ifPresent(existing -> stepDefinition.setId(existing.getId()));
        return store.saveStep(stepDefinition);
    }

    public List<StepDefinition> list() {
        return store.findSteps();
    }

    public StepDefinition findByStepCode(String tenant, String stepCode) {
        return store.findStep(tenant, stepCode).orElse(null);
    }

    /**
     * The step as the process uses it: the tenant's shared definition, overridden by the fields the process's own
     * {@code steps} entry sets. Either may be absent.
     *
     * @param processCode the process, or {@code null} for the shared definition alone
     */
    public StepDefinition findStepDefinition(String tenant, String processCode, String stepCode) {
        return resolve(findByStepCode(tenant, stepCode), processStep(tenant, processCode, stepCode));
    }

    /**
     * {@code shared} overridden by {@code inProcess}; either may be {@code null}.
     */
    public static StepDefinition resolve(StepDefinition shared, StepDefinition inProcess) {
        if (shared == null) {
            return inProcess;
        }
        return inProcess == null ? shared : shared.overriddenBy(inProcess);
    }

    private StepDefinition processStep(String tenant, String processCode, String stepCode) {
        if (processCode == null) {
            return null;
        }
        return store.findProcess(tenant, processCode).stream()
                .filter(process -> process.getSteps() != null)
                .flatMap(process -> process.getSteps().stream())
                .filter(step -> stepCode.equals(step.getStepCode()))
                .findFirst()
                .orElse(null);
    }
}
