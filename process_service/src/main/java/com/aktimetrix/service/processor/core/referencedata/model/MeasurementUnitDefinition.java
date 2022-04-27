package com.aktimetrix.service.processor.core.referencedata.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "measurementUnitDefinitions")
public class MeasurementUnitDefinition {

    private String tenant;
    @Id
    private String id;
    private String code;
    private String name;
    private String category;
}
