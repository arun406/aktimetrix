package com.aktimetrix.aktimetrix.reference.data.controller;

import com.aktimetrix.aktimetrix.reference.data.dto.LineDTO;
import com.aktimetrix.aktimetrix.reference.data.dto.RouteDTO;
import com.aktimetrix.aktimetrix.reference.data.service.LineService;
import com.aktimetrix.aktimetrix.reference.data.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/encore/lines")
public class LinesController {

    @Autowired
    private LineService lineService;

    @GetMapping
    public List<LineDTO> getAll() {
        return lineService.list();
    }
}
