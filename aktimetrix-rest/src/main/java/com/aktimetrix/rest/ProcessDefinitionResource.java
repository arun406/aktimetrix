package com.aktimetrix.rest;

import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import com.aktimetrix.core.service.ProcessMigrationService;
import com.aktimetrix.core.service.ProcessMigrationService.Migration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Process definitions: listed, created or replaced by tenant and process code, and their running instances migrated to
 * the current revision.
 */
@RestController
@RequestMapping("/reference-data/process-definitions")
@Tag(name = "Process definitions", description = "The process definition: its entity type, the events that start, end and cancel it, its steps, measurements, metrics and deadline")
public class ProcessDefinitionResource {

    private final ProcessDefinitionService service;
    private final ProcessMigrationService migrations;

    public ProcessDefinitionResource(ProcessDefinitionService service, ProcessMigrationService migrations) {
        this.service = service;
        this.migrations = migrations;
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

    @PostMapping("/{tenant}/{processCode}/migrations")
    @Operation(summary = "Migrate the running instances to the current revision",
            description = "Moves every running instance of the process that follows an older revision to the current one, "
                    + "each in its own unit of work: steps the revision adds are created and planned, steps it removes are "
                    + "skipped while open, and steps still awaited are planned again from the new durations and tolerances. "
                    + "What already happened is kept. Each migrated instance publishes a MIGRATED process event.")
    @ApiResponse(responseCode = "200", description = "Migrated; instances that failed keep their revision and are listed")
    @ApiResponse(responseCode = "404", description = "The tenant has no definition of the process")
    public ResponseEntity<Migration> migrate(@PathVariable String tenant, @PathVariable String processCode) {
        if (service.findByCode(tenant, processCode) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(migrations.migrate(tenant, processCode));
    }
}
