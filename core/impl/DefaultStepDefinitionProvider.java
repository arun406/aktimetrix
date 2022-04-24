package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.DefinitionProvider;
import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;

import java.util.List;

public class DefaultStepDefinitionProvider implements DefinitionProvider<StepDefinition> {

    private final ProcessDefinition processDefinition;

    public DefaultStepDefinitionProvider(ProcessDefinition processDefinition) {
        this.processDefinition = processDefinition;
    }

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
}
