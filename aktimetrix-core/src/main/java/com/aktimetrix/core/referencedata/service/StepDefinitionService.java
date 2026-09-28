package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.repository.ProcessDefinitionRepository;
import com.aktimetrix.core.referencedata.repository.StepDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StepDefinitionService {

    @Autowired
    StepDefinitionRepository repository;
    @Autowired
    ProcessDefinitionRepository processDefinitionRepository;

    /**
     * Saves a new Step Definition
     *
     * @param stepDefinition
     * @return
     */
    /**
     * Saves the step definition, replacing an existing one with the same tenant and step code.
     */
    public StepDefinition add(StepDefinition stepDefinition) {
        StepDefinition existing = repository.findByStepCode(stepDefinition.getTenant(), stepDefinition.getStepCode());
        if (existing != null) {
            stepDefinition.setId(existing.getId());
        }
        repository.save(stepDefinition);
        return stepDefinition;
    }

    /**
     * Returns all Step Definitions
     *
     * @return
     */
    public List<StepDefinition> list() {
        return this.repository.findAll();
    }

    /**
     * @return
     */
    public StepDefinition findByStepCode(String tenant, String stepCode) {
        return this.repository.findByStepCode(tenant, stepCode);
    }

    /**
     * The definition of a step as the process uses it: the tenant's shared step definition, with the fields set on
     * the step in the process definition overriding it. A step may also be defined only in the process.
     *
     * @return the definition, or {@code null} when the step is defined neither in the process nor for the tenant
     */
    public StepDefinition findStepDefinition(String tenant, String processCode, String stepCode) {
        return resolve(repository.findByStepCode(tenant, stepCode), processStep(tenant, processCode, stepCode));
    }

    /**
     * Combines a shared step definition with the step as written in a process definition; either may be absent.
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
        return processDefinitionRepository.findByTenantAndProcessCode(tenant, processCode).stream()
                .filter(process -> process.getSteps() != null)
                .flatMap(process -> process.getSteps().stream())
                .filter(step -> stepCode.equals(step.getStepCode()))
                .findFirst()
                .orElse(null);
    }
}
