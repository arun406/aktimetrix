package com.aktimetrix.service.processor.core.impl;

import com.aktimetrix.service.processor.core.api.DefinitionProvider;
import com.aktimetrix.service.processor.core.exception.DefinitionNotFoundException;
import com.aktimetrix.service.processor.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.service.processor.core.referencedata.model.StepDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class DefaultStepDefinitionProvider implements DefinitionProvider<StepDefinition> {

    private ProcessDefinition processDefinition;

    public DefaultStepDefinitionProvider() {
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
