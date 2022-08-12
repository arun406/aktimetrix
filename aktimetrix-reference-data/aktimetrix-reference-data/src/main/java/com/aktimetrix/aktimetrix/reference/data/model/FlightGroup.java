package com.aktimetrix.aktimetrix.reference.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "flightGroups")
public class FlightGroup {

    @Id
    private String id;
    private String airline;
    private String flightNumber;
    private String flightGroupCode;
    private String flightGroupName;

    public FlightGroup(String airline, String flightNumber, String flightGroupCode, String flightGroupName) {
        this.airline = airline;
        this.flightNumber = flightNumber;
        this.flightGroupCode = flightGroupCode;
        this.flightGroupName = flightGroupName;
    }
}

