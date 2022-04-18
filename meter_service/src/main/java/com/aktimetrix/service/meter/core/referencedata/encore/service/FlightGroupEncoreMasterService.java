package com.aktimetrix.service.meter.core.referencedata.encore.service;

import com.aktimetrix.service.meter.core.referencedata.encore.model.FlightGroupEncoreMaster;
import com.aktimetrix.service.meter.core.referencedata.encore.repository.FlightGroupEncoreMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightGroupEncoreMasterService {

    @Autowired
    private FlightGroupEncoreMasterRepository repository;

    /**
     * Adds a new Flight Group Encore Master Record
     *
     * @param flightGroupEncoreMaster
     * @return
     */
    public FlightGroupEncoreMaster add(FlightGroupEncoreMaster flightGroupEncoreMaster) {
        repository.save(flightGroupEncoreMaster);
        return flightGroupEncoreMaster;
    }


    /**
     * returns all FlightGroupEncoreMaster records
     *
     * @return
     */
    public List<FlightGroupEncoreMaster> list() {
        return repository.findAll();
    }

    /**
     * Returns the Flight Group Records by flight No
     *
     * @param flightNo
     * @return
     */
    public List<FlightGroupEncoreMaster> listByFlightNo(String flightNo, String flightGroupCode) {
        return repository.findByFlightNoAndFlightGroupCode(flightNo, flightGroupCode);
    }


    /**
     * returns the flight group codes of flight number
     *
     * @param flightNo
     * @return
     */
    public List<String> getFlightGroupCodes(String flightNo) {
        final List<FlightGroupEncoreMaster> flightGroups = this.repository.findByFlightNo(flightNo);
        if (flightGroups != null && !flightGroups.isEmpty()) {
            return flightGroups.stream()
                    .map(FlightGroupEncoreMaster::getFlightGroupCode)
                    .collect(Collectors.toList());
        }
        return null;
    }
}
