package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.DefinitionProvider;
import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Builder
public class DefaultProcessDefinitionProvider implements DefinitionProvider<ProcessDefinition> {

    @Autowired
    private ProcessDefinitionService processDefinitionService;

    @Override
    public List<ProcessDefinition> getDefinitions() throws DefinitionNotFoundException {
        final List<ProcessDefinition> list = this.processDefinitionService.list();
        if (list == null) {
            return new ArrayList<>();
        }
        return list;
    }
}
