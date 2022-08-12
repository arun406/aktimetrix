package com.aktimetrix.aktimetrix.reference.data.controller;

import com.aktimetrix.aktimetrix.reference.data.dto.FlightGroupDTO;
import com.aktimetrix.aktimetrix.reference.data.dto.RouteDTO;
import com.aktimetrix.aktimetrix.reference.data.service.FlightGroupService;
import com.aktimetrix.aktimetrix.reference.data.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/encore/routes")
public class RoutesController {

    @Autowired
    private RouteService routeService;

    @GetMapping
    public List<RouteDTO> getAll() {
        return routeService.list();
    }
}
