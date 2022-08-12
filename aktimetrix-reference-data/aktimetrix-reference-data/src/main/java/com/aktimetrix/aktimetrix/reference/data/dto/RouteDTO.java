package com.aktimetrix.aktimetrix.reference.data.dto;

import lombok.Data;

@Data
public class RouteDTO {
    private String airline;
    private String forwarder;
    private String origin;
    private String destination;

    public RouteDTO(String airline, String forwarder, String origin, String destination) {
        this.airline = airline;
        this.forwarder = forwarder;
        this.origin = origin;
        this.destination = destination;
    }
}
