package com.aktimetrix.core.referencedata.resource;

import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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
    public List<ProcessDefinition> list(@RequestParam(value = "processType", required = false) String processType, @RequestParam(value = "processCode", required = false) String processCode) throws DefinitionNotFoundException {
        if (StringUtils.hasText(processType) || StringUtils.hasText(processCode)) {
            return this.service.get(processType, processCode);
        }
        return this.service.list();
    }
}
