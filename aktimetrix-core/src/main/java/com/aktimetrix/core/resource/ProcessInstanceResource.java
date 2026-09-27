package com.aktimetrix.core.resource;

import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.service.ProcessInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Answers "where is this business entity?": its process instances, each with its steps' status, planned and actual
 * times, and timeliness.
 * <pre>
 * GET /process-instances?tenant=AA&amp;entityId=1234[&amp;entityType=com.ecom.order]
 * </pre>
 */
@RestController
@RequestMapping("/process-instances")
@RequiredArgsConstructor
public class ProcessInstanceResource {

    private final ProcessInstanceService processInstanceService;

    @GetMapping
    public List<ProcessInstance> find(@RequestParam String tenant, @RequestParam String entityId,
                                      @RequestParam(required = false) String entityType) {
        return processInstanceService.getProcessInstancesWithSteps(tenant, entityType, entityId);
    }
}
