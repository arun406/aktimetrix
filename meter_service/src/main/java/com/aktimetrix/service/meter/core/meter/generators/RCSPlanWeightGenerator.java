package com.aktimetrix.service.meter.core.meter.generators;

import com.aktimetrix.service.meter.core.meter.impl.AbstractMeter;
import com.aktimetrix.service.meter.core.stereotypes.Measurement;
import com.aktimetrix.service.meter.core.transferobjects.StepInstanceDTO;
import org.springframework.stereotype.Component;

@Component
@Measurement(code = "WT", stepCode = "RCS")
public class RCSPlanWeightGenerator extends AbstractMeter {

    @Override
    protected String getMeasurementUnit(String tenant, StepInstanceDTO step) {
        return String.valueOf((double) step.getMetadata().get("reservationWeight"));
    }

    @Override
    protected String getMeasurementValue(String tenant, StepInstanceDTO step) {
        return (String) step.getMetadata().get("reservationWeightUnit");
    }
}
