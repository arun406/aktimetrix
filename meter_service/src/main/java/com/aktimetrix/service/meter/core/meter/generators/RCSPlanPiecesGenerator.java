package com.aktimetrix.service.meter.core.meter.generators;

import com.aktimetrix.service.meter.core.meter.impl.AbstractMeter;
import com.aktimetrix.service.meter.core.stereotypes.Measurement;
import com.aktimetrix.service.meter.core.transferobjects.StepInstanceDTO;
import org.springframework.stereotype.Component;

@Component
@Measurement(code = "PCS", stepCode = "RCS")
public class RCSPlanPiecesGenerator extends AbstractMeter {

    @Override
    protected String getMeasurementUnit(String tenant, StepInstanceDTO step) {
        return String.valueOf((int) step.getMetadata().get("reservationPieces"));
    }

    @Override
    protected String getMeasurementValue(String tenant, StepInstanceDTO step) {
        return "N";
    }
}