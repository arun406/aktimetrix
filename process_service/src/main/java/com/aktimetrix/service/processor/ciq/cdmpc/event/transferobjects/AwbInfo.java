package com.aktimetrix.service.processor.ciq.cdmpc.event.transferobjects;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AwbInfo implements Serializable {
    private String documentNumber;
    private String documentPrefix;
    private String documentType;
    private boolean eAWBIndicator;
    private StationInfo origin;
    private StationInfo destination;
    private List<Participant> participant;
}
