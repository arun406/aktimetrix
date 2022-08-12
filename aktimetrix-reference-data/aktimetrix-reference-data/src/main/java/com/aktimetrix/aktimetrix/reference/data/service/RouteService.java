package com.aktimetrix.aktimetrix.reference.data.service;

import com.aktimetrix.aktimetrix.reference.data.dto.RouteDTO;
import com.aktimetrix.aktimetrix.reference.data.model.Route;
import com.aktimetrix.aktimetrix.reference.data.repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RouteService {
    @Autowired
    private RouteRepository repository;

    public void cleanup() {
        this.repository.deleteAll();
    }

    public void saveAll(List<RouteDTO> dtos) {
        List<Route> routes = dtos.stream()
                .map(dto -> new Route(dto.getAirline(), dto.getForwarder(), dto.getOrigin(), dto.getDestination()))
                .collect(Collectors.toList());
        this.repository.saveAll(routes);
    }

    public void save(RouteDTO dto) {
        this.repository.save(new Route(dto.getAirline(), dto.getForwarder(), dto.getOrigin(), dto.getDestination()));
    }

    public List<RouteDTO> list() {
        List<Route> routes = this.repository.findAll();
        if (routes == null) {
            return new ArrayList<>();
        }
        return routes.stream()
                .map(m -> new RouteDTO(m.getAirline(), m.getForwarder(), m.getOrigin(), m.getDestination()))
                .collect(Collectors.toList());
    }
}
