package com.aktimetrix.service.processor.ciq.cdmpc.referencedata.encore.resource;

import com.aktimetrix.service.processor.ciq.cdmpc.referencedata.encore.model.FlightGroupEncoreMaster;
import com.aktimetrix.service.processor.ciq.cdmpc.referencedata.encore.service.FlightGroupEncoreMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;


@RequestMapping("/reference-data/encore/flight-groups")
@RestController
public class FlightGroupCodeResource {

    @Autowired
    private FlightGroupEncoreMasterService service;

    /**
     * @param flightGroupEncoreMaster
     * @return
     */
    @PostMapping
    public ResponseEntity add(FlightGroupEncoreMaster flightGroupEncoreMaster) {
        service.add(flightGroupEncoreMaster);
        return ResponseEntity.created(URI.create("/reference-data/encore/flight-groups/" + flightGroupEncoreMaster.getId())).build();
    }

    @GetMapping
    public List<FlightGroupEncoreMaster> list(@RequestParam(value = "flightGroupCode", required = false) String flightGroupCode,
                                              @RequestParam(value = "flightNo", required = false) String flightNo) {
        if (flightNo != null && flightGroupCode != null) {
            return service.listByFlightNo(flightNo, flightGroupCode);
        } else {
            return service.list();
        }
    }
}
