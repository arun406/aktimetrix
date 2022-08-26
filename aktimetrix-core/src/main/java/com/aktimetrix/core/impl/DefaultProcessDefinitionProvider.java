package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.DefinitionProvider;
import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
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

    /**
     * is the event code is start event
     *
     * @param tenant
     * @param eventCode
     * @return
     */
    public boolean isProcessStartEvents(String tenant, String eventCode) {
        return this.processDefinitionService.list().stream()
                .filter(pd -> pd.getTenant().equals(tenant))
                .map(ProcessDefinition::getStartEventCodes)
                .anyMatch(events -> events.contains(eventCode));
    }

    /**
     * returns process definition matching event code
     *
     * @param tenant
     * @param eventCode
     * @return
     * @throws DefinitionNotFoundException
     */
    public List<ProcessDefinition> getDefinitions(String tenant, String eventCode) {
        return this.processDefinitionService.getByEventCode(tenant, eventCode);
    }
}
