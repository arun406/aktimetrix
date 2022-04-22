package com.aktimetrix.service.processor.ciq.cdmpc.event.transferobjects;

import lombok.Data;

import java.util.List;

@Data
public class Cargo {
    private String cargoType;
    private String cargoCategory;
    private String commodity;
    private String description;
    private Long jobReferenceNumber;
    private Long cargoReference;
    private boolean eAWBIndicator;
    private StationInfo origin;
    private StationInfo destination;
    private String productCode;
    private List<Remark> remarks;
    private List<SpecialHandling> shcList;
    private List<String> reservationServiceCodes;
    private List<Itinerary> itineraries;
    private List<QuantityInfo> quantityInfo;
    private DocumentInfo documentInfo;
}
