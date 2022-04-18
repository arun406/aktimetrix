package com.aktimetrix.service.processor.ciq.cdmpc.referencedata.encore.resource;

import com.aktimetrix.service.processor.ciq.cdmpc.referencedata.encore.model.RouteEncoreMaster;
import com.aktimetrix.service.processor.ciq.cdmpc.referencedata.encore.service.RouteEncoreMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RequestMapping("/reference-data/encore/routes")
@RestController
public class RouteEncoreMasterResource {

    @Autowired
    RouteEncoreMasterService service;

    @GetMapping
    public List<RouteEncoreMaster> list() {
        return service.list();
    }

    @PostMapping
    public ResponseEntity add(RouteEncoreMaster encoreMaster) {
        service.add(encoreMaster);
        return ResponseEntity.created(URI.create("/reference-data/encore/routes/" + encoreMaster.getId())).build();
    }
}
