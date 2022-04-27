package com.aktimetrix.service.processor.core.referencedata.service;

import com.aktimetrix.service.processor.core.referencedata.model.StepDefinition;
import com.aktimetrix.service.processor.core.referencedata.repository.StepDefinitionRepository;
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
    public StepDefinition add(StepDefinition stepDefinition) {
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
