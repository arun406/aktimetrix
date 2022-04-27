package com.aktimetrix.core.referencedata.resource;

import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/reference-data/process-definitions")
public class ProcessDefinitionResource {

    @Autowired
    ProcessDefinitionService service;

    @PostMapping
    public ResponseEntity add(ProcessDefinition definition) {
        this.service.add(definition);
        return ResponseEntity.created(URI.create("/reference-data/process-definitions/" + definition.getId())).build();
    }

    @GetMapping
    public List<ProcessDefinition> list() {
        return this.service.list();
    }
}
