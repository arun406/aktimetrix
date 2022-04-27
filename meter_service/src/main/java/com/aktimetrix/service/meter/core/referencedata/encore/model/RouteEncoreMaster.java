package com.aktimetrix.service.meter.core.referencedata.encore.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@ToString
@Document(collection = "routes")
public class RouteEncoreMaster {

    private String tenant;
    @Id
    private String id;
    private String airline;
    private String forwarder;
    private String origin;
    private String destination;
    private String status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate confirmedDate;
}
