package com.aktimetrix.core.referencedata.resource;

import com.aktimetrix.core.referencedata.model.MeasurementTypeDefinition;
import com.aktimetrix.core.referencedata.service.MeasurementTypeDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RequestMapping("/reference-data/measurement-type-definitions")
@RestController
public class MeasurementTypeDefinitionsResource {

    @Autowired
    MeasurementTypeDefinitionService service;

    @GetMapping
    public List<MeasurementTypeDefinition> list() {
        return this.service.list();
    }

    @PostMapping
    public ResponseEntity add(@RequestBody MeasurementTypeDefinition definition) {
        this.service.add(definition);
        return ResponseEntity.created(URI.create("/reference-data/measurement-type-definitions/" + definition.getId())).build();
    }
}
