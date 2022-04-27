package com.aktimetrix.core.referencedata.resource;

import com.aktimetrix.core.referencedata.model.EventTypeDefinition;
import com.aktimetrix.core.referencedata.service.EventTypeDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;

//@RestController
//@RequestMapping("/reference-data/event-type-definitions")
public class EventTypeDefinitionResource {


    @Autowired
    EventTypeDefinitionService service;

    //    @PostMapping
    public ResponseEntity add(EventTypeDefinition definition) {
        this.service.add(definition);
        return ResponseEntity.created(URI.create("/reference-data/event-type-definitions/" + definition.getId())).build();
    }

    //    @GetMapping
    public List<EventTypeDefinition> list() {
        return this.service.list();
    }
}
