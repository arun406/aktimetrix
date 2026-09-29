package com.aktimetrix.core.referencedata.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class MeasurementUnitDefinition {

    private String tenant;
    @Id
    private String id;
    private String code;
    private String name;
    private String category;
}
