package com.aktimetrix.aktimetrix.reference.data.controller;

import com.aktimetrix.aktimetrix.reference.data.dto.FlightGroupDTO;
import com.aktimetrix.aktimetrix.reference.data.service.FlightGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/encore/flight-groups")
public class FlightGroupController {

    @Autowired
    private FlightGroupService flightGroupService;

    @GetMapping
    public List<FlightGroupDTO> getAll() {
        return flightGroupService.list();
    }
}
