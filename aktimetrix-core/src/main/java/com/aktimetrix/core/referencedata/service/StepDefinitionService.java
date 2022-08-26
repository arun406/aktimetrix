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
    public StepDefinition get(String tenant, String stepCode, String status) {
        return this.repository.findByStepCodeAndStatus(tenant, stepCode, status);
    }

    /**
     * @return
     */
    public StepDefinition get(String tenant, String stepCode) {
        return this.repository.findByStepCodeAndStatus(tenant, stepCode, "CONFIRMED");
    }

    public List<StepDefinition> getStepByStartEvent(String tenant, String eventCode) {
        return this.repository.findByStartEventCode(tenant, eventCode);
    }
}
