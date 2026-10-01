package com.aktimetrix.rest;

import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.service.StepDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Step definitions: listed, and created or replaced by tenant and step code.
 */
@RestController
@RequestMapping("/reference-data/step-definitions")
@Tag(name = "Step definitions", description = "The step definition shared by the tenant's processes: the events that start, complete and report progress on it, its plan and measurements")
public class StepDefinitionResource {

    private final StepDefinitionService service;

    public StepDefinitionResource(StepDefinitionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create or replace a step definition",
            description = "Replaces the definition with the same tenant and step code. The definition is validated first, as "
                    + "strictly as a definition file.")
    @ApiResponse(responseCode = "201", description = "Saved")
    @ApiResponse(responseCode = "400", description = "Invalid: nothing was saved",
            content = @Content(schema = @Schema(implementation = DefinitionProblems.class)))
    public ResponseEntity<Void> add(@RequestBody StepDefinition definition) {
        final StepDefinition saved = service.add(definition);
        return ResponseEntity.created(URI.create("/reference-data/step-definitions/" + saved.getId())).build();
    }

    @GetMapping
    @Operation(summary = "List the step definitions", description = "Of every tenant.")
    public List<StepDefinition> list() {
        return service.list();
    }
}
