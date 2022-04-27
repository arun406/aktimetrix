package com.aktimetrix.service.meter.core.referencedata.encore.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@Data
@Document(collection = "lines")
public class LinesEncoreMaster {

    private String tenant;
    @Id
    private String id;
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
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate confirmedDate;
    private String activeInd;

    private List<LineOffsetEncoreMaster> offsets;
}
