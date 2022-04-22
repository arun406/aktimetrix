package com.aktimetrix.core.referencedata.model;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@ToString
@Document(collection = "measurementTypeDefinitions")
public class MeasurementTypeDefinition {

    private String tenant;
    @Id
    private String id;
    private String code;
    private String name;
    private String unitCode;
    private MeasurementUnitDefinition unit;
}
