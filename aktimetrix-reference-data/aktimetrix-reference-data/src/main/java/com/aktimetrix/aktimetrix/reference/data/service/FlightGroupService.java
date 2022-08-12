package com.aktimetrix.aktimetrix.reference.data.service;

import com.aktimetrix.aktimetrix.reference.data.dto.FlightGroupDTO;
import com.aktimetrix.aktimetrix.reference.data.model.FlightGroup;
import com.aktimetrix.aktimetrix.reference.data.repository.FlightGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightGroupService {

    @Autowired
    private FlightGroupRepository flightGroupRepository;

    public void cleanup() {
        this.flightGroupRepository.deleteAll();
    }

    public void save(List<FlightGroupDTO> flightGroupDTOs) {
        List<FlightGroup> flightGroups = flightGroupDTOs.stream()
                .map(dto -> new FlightGroup(dto.getAirline(), dto.getFlightNumber(), dto.getFlightGroupCode(), dto.getFlightGroupName()))
                .collect(Collectors.toList());
        this.flightGroupRepository.saveAll(flightGroups);
    }


    public void save(FlightGroupDTO dto) {
        FlightGroup flightGroup = new FlightGroup(dto.getAirline(), dto.getFlightNumber(), dto.getFlightGroupCode(), dto.getFlightGroupName());
        this.flightGroupRepository.save(flightGroup);
    }

    /**
     * @return
     */
    public List<FlightGroupDTO> list() {
        List<FlightGroup> all = this.flightGroupRepository.findAll();
        if (all == null) {
            return new ArrayList<>();
        }
        return all.stream()
                .map(m -> new FlightGroupDTO(m.getAirline(), m.getFlightNumber(), m.getFlightGroupCode(), m.getFlightGroupName()))
                .collect(Collectors.toList());
    }

}
