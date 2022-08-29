package com.aktimetrix.core.referencedata.resource;

import com.aktimetrix.core.referencedata.model.MeasurementUnitDefinition;
import com.aktimetrix.core.referencedata.service.MeasurementUnitDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RequestMapping("/reference-data/measurement-unit-definitions")
@RestController
public class MeasurementUnitDefinitionsResource {

    @Autowired
    MeasurementUnitDefinitionService service;

    @GetMapping
    public List<MeasurementUnitDefinition> list() {
        return this.service.list();
    }

    @PostMapping
    public ResponseEntity add(@RequestBody MeasurementUnitDefinition definition) {
        this.service.add(definition);
        return ResponseEntity.created(URI.create("/reference-data/measurement-unit-definitions/" + definition.getId())).build();
    }
}
