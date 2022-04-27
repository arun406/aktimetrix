package com.aktimetrix.core.referencedata.model;

import com.aktimetrix.core.api.MeasurementType;
import lombok.Data;

@Data
public class StepMeasurement {
    private String measurementCode;
    private MeasurementType type;
}
