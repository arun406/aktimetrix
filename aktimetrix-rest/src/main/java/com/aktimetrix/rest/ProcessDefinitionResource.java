package com.aktimetrix.rest;

import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
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
 * Process definitions: listed, and created or replaced by tenant and process code.
 */
@RestController
@RequestMapping("/reference-data/process-definitions")
@Tag(name = "Process definitions", description = "The process definition: its entity type, the events that start, end and cancel it, its steps, measurements, metrics and deadline")
public class ProcessDefinitionResource {

    private final ProcessDefinitionService service;

    public ProcessDefinitionResource(ProcessDefinitionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create or replace a process definition",
            description = "Replaces the definition with the same tenant and process code. The definition is validated first, as "
                    + "strictly as a definition file. A changed definition becomes a new revision; process instances already running keep the revision they started with.")
    @ApiResponse(responseCode = "201", description = "Saved")
    @ApiResponse(responseCode = "400", description = "Invalid: nothing was saved",
            content = @Content(schema = @Schema(implementation = DefinitionProblems.class)))
    public ResponseEntity<Void> add(@RequestBody ProcessDefinition definition) {
        final ProcessDefinition saved = service.add(definition);
        return ResponseEntity.created(URI.create("/reference-data/process-definitions/" + saved.getId())).build();
    }

    @GetMapping
    @Operation(summary = "List the process definitions", description = "Of every tenant.")
    public List<ProcessDefinition> list() {
        return service.list();
    }
}
