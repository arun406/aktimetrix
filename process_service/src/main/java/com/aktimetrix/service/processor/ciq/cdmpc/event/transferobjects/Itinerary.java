package com.aktimetrix.service.processor.ciq.cdmpc.event.transferobjects;

import lombok.Data;

@Data
public class Itinerary {
    private StationInfo boardPoint;
    private StationInfo offPoint;
    private TransportInfo transportInfo;
    private TransportTime departureDateTimeLocal;
    private TransportTime departureDateTimeUTC;
    private TransportTime arrivalDateTimeLocal;
    private TransportTime arrivalDateTimeUTC;
    private int legNumber;
    private boolean partIndicator;
    private boolean oalIndicator;
    private QuantityInfo quantity;
    private String allotmentId;
    private CodeValue movementStatus;
    private CodeValue spaceStatus;
    private String aircraftCategory;
}
