package com.aktimetrix.rest;

import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.service.ProcessInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Answers "where is this business entity?": its process instances, each with its steps' status, planned and actual
 * times, and timeliness.
 */
@RestController
@RequestMapping("/process-instances")
@Tag(name = "Process instances", description = "Where a business entity stands against its plan")
public class ProcessInstanceResource {

    private final ProcessInstanceService processInstanceService;

    public ProcessInstanceResource(ProcessInstanceService processInstanceService) {
        this.processInstanceService = processInstanceService;
    }

    @GetMapping
    @Operation(summary = "The process instances of a business entity",
            description = "Each process instance with its steps: status, planned, expected and actual times, and "
                    + "timeliness. Empty when no process has started for the entity.")
    public List<ProcessInstance> find(
            @Parameter(description = "Tenant of the entity", example = "AA") @RequestParam String tenant,
            @Parameter(description = "Id of the business entity", example = "1234") @RequestParam String entityId,
            @Parameter(description = "Type of the business entity, to tell apart entities of different types with "
                    + "the same id", example = "com.ecom.order") @RequestParam(required = false) String entityType) {
        return processInstanceService.getProcessInstancesWithSteps(tenant, entityType, entityId);
    }
}
