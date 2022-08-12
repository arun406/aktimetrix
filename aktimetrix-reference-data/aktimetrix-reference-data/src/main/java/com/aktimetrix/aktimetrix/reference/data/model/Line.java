package com.aktimetrix.aktimetrix.reference.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "lines")
public class Line {
    @Id
    private String id;
    private String airline;
    private String airport;
    private String exportInd;
    private String importInd;
    private String transitInd;
    private String productCode;
    private String productGroupCode;
    private String forwarderCode;
    private String dow;
    private String flightNo;
    private String flightGroupCode;
    private String acCategory;
    private String fohBeforeFwbInd;
    private String wtInd;
    private String volInd;
    private String eFreightInd;
    private String notes;
    private String FWB;
    private String LAT;
    private String RCS;
    private String DEP;
    private String ARR;
    private String AWR;
    private String RCF;
    private String NFD;
    private String AWD;
    private String DLV;
    private String RCFT;
    private String TFDT;
    private String ARRT;
    private String RCTT;
    private String DEPT;
}
