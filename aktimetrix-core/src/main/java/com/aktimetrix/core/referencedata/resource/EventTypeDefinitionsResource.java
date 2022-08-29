package com.aktimetrix.core.referencedata.resource;

import com.aktimetrix.core.referencedata.model.EventTypeDefinition;
import com.aktimetrix.core.referencedata.service.EventTypeDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RequestMapping("/reference-data/event-type-definitions")
@RestController
public class EventTypeDefinitionsResource {

    @Autowired
    EventTypeDefinitionService service;

    @GetMapping
    public List<EventTypeDefinition> list() {
        return this.service.list();
    }

    @PostMapping
    public ResponseEntity add(@RequestBody EventTypeDefinition definition) {
        this.service.add(definition);
        return ResponseEntity.created(URI.create("/reference-data/event-type-definitions/" + definition.getId())).build();
    }
}
