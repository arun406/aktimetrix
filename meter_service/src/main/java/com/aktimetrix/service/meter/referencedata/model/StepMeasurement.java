package com.aktimetrix.service.meter.referencedata.model;

import com.aktimetrix.service.meter.referencedata.transferobjects.MeasurementType;
import lombok.Data;

@Data
public class StepMeasurement {
    private String measurementCode;
    private MeasurementType type;
}
