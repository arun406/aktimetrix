package com.aktimetrix.rest;

import com.aktimetrix.core.referencedata.model.MeasurementTypeDefinition;
import com.aktimetrix.core.referencedata.service.MeasurementTypeDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * The catalogue of measurement types, for tools and consumers; the runtime does not require it.
 */
@RestController
@RequestMapping("/reference-data/measurement-type-definitions")
@Tag(name = "Measurement types", description = "A catalogue of measurement types with their units, for tools and "
        + "consumers; measurements are defined by code and unit where they are used, without it")
public class MeasurementTypeDefinitionsResource {

    private final MeasurementTypeDefinitionService service;

    public MeasurementTypeDefinitionsResource(MeasurementTypeDefinitionService service) {
        this.service = service;
    }

    @PreAuthorize("@aktimetrixRestAccess.canRead(authentication)")
    @GetMapping
    @Operation(summary = "List the measurement types")
    public List<MeasurementTypeDefinition> list() {
        return service.list();
    }

    @PreAuthorize("@aktimetrixRestAccess.canWrite(authentication)")
    @PostMapping
    @Operation(summary = "Register a measurement type")
    @ApiResponse(responseCode = "201", description = "Saved")
    public ResponseEntity<Void> add(@RequestBody MeasurementTypeDefinition definition) {
        final MeasurementTypeDefinition saved = service.add(definition);
        return ResponseEntity.created(URI.create("/reference-data/measurement-type-definitions/" + saved.getId()))
                .build();
    }
}
