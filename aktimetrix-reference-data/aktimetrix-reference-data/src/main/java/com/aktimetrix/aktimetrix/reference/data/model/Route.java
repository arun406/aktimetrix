package com.aktimetrix.aktimetrix.reference.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "routes")
public class Route {
    @Id
    private String id;
    private String airline;
    private String forwarder;
    private String origin;
    private String destination;

    public Route(String airline, String forwarder, String origin, String destination) {
        this.airline = airline;
        this.forwarder = forwarder;
        this.origin = origin;
        this.destination = destination;
    }
}
