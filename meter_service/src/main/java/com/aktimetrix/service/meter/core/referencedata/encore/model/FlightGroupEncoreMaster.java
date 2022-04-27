package com.aktimetrix.service.meter.core.referencedata.encore.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Document(collection = "flightGroups")
public class FlightGroupEncoreMaster {

    private String tenant;
    @Id
    private String id;
    private String flightNo;
    private String flightGroupCode;
    private String flightGroupName;
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate confirmedDate;

}
