package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.repository.StepDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StepDefinitionService {

    @Autowired
    StepDefinitionRepository repository;

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
}
