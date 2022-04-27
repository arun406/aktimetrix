package com.aktimetrix.service.processor.core.referencedata.service;

import com.aktimetrix.service.processor.core.referencedata.model.EventTypeDefinition;
import com.aktimetrix.service.processor.core.referencedata.repository.EventTypeDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventTypeDefinitionService {

    @Autowired
    EventTypeDefinitionRepository repository;

    /**
     * Saves a new Event Type Definition
     *
     * @param eventTypeDefinition
     * @return
     */
    public EventTypeDefinition add(EventTypeDefinition eventTypeDefinition) {
        repository.save(eventTypeDefinition);
        return eventTypeDefinition;
    }

    /**
     * @return
     */
    public List<EventTypeDefinition> list() {
        return this.repository.findAll();
    }
}
