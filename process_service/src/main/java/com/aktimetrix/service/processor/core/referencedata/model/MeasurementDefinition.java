package com.aktimetrix.service.processor.core.referencedata.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "measurementDefinitions")
public class MeasurementDefinition {

    private String tenant;

    @Id
    private String id;
    private String measurementCode;
    private String measurementName;
    private String applicableTo;
    private MeasurementTypeDefinition type;
}
