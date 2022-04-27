package com.aktimetrix.service.processor.core.referencedata.resource;

import com.aktimetrix.service.processor.core.referencedata.model.StepDefinition;
import com.aktimetrix.service.processor.core.referencedata.service.StepDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/reference-data/step-definitions")
public class StepDefinitionResource {

    @Autowired
    StepDefinitionService service;

    @PostMapping
    public ResponseEntity add(@RequestBody StepDefinition definition) {
        this.service.add(definition);
        return ResponseEntity.created(URI.create("/reference-data/step-definitions/" + definition.getId())).build();
    }


    @GetMapping
    public List<StepDefinition> list() {
        return this.service.list();
    }
}
