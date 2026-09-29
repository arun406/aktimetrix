package com.aktimetrix.core.referencedata.model;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;

@Data
@ToString
public class MeasurementTypeDefinition {

    private String tenant;
    @Id
    private String id;
    private String code;
    private String name;
    private String unitCode;
    private MeasurementUnitDefinition unit;
}
