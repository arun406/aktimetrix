package com.aktimetrix.core.referencedata.model;

import com.aktimetrix.core.api.MeasurementType;
import lombok.Data;

/**
 * A measurement declared on a step or process definition: its code and whether it is planned or actual.
 */
@Data
public class StepMeasurement {
    private String measurementCode;
    private MeasurementType type;
}
