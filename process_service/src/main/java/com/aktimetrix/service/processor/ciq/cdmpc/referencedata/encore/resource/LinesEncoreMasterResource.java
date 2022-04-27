package com.aktimetrix.service.processor.ciq.cdmpc.referencedata.encore.resource;

import com.aktimetrix.service.processor.ciq.cdmpc.referencedata.encore.model.LinesEncoreMaster;
import com.aktimetrix.service.processor.ciq.cdmpc.referencedata.encore.service.LinesEncoreMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RequestMapping("/reference-data/encore/lines")
@RestController
public class LinesEncoreMasterResource {

    @Autowired
    LinesEncoreMasterService service;

    @PostMapping
    public ResponseEntity add(LinesEncoreMaster linesEncoreMaster) {
        service.add(linesEncoreMaster);
        return ResponseEntity.created(URI.create("/reference-data/encore/lines/" + linesEncoreMaster.getId())).build();
    }

    @GetMapping
    public List<LinesEncoreMaster> list() {
        return service.list();
    }
}
