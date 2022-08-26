package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.DefinitionProvider;
import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.service.StepDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultStepDefinitionProvider implements DefinitionProvider<StepDefinition> {

    private ProcessDefinition processDefinition;
    @Autowired
    private StepDefinitionService stepDefinitionService;

    /**
     * returns the  step  definitions
     *
     * @return definition collection
     * @throws DefinitionNotFoundException
     */
    @Override
    public List<StepDefinition> getDefinitions() throws DefinitionNotFoundException {
        if (!processDefinition.getSteps().isEmpty()) {
            return processDefinition.getSteps();
        }
        // get the step definitions from database
        return null;
    }

    public boolean isStepStartEvent(String tenant, String eventCode) {
        final List<StepDefinition> stepByStartEvent = this.stepDefinitionService.getStepByStartEvent(tenant, eventCode);
        if (stepByStartEvent == null || stepByStartEvent.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * returns the step definitions
     *
     * @param tenant
     * @param eventCode
     * @return
     */
    public List<StepDefinition> getDefinitions(String tenant, String eventCode) {
        return this.stepDefinitionService.getStepByStartEvent(tenant, eventCode);
    }
}
