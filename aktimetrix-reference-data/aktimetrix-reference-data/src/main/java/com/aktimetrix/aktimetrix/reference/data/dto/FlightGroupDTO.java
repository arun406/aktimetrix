package com.aktimetrix.aktimetrix.reference.data.dto;

import lombok.Data;

@Data
public class FlightGroupDTO {
    private String airline;
    private String flightNumber;
    private String flightGroupCode;
    private String flightGroupName;

    public FlightGroupDTO(String airline, String flightNumber, String flightGroupCode, String flightGroupName) {
        this.airline = airline;
        this.flightNumber = flightNumber;
        this.flightGroupCode = flightGroupCode;
        this.flightGroupName = flightGroupName;
    }
}
