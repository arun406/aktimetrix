package com.aktimetrix.service.meter.core.meter.generators;

import com.aktimetrix.core.meter.impl.AbstractMeter;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.stereotypes.Measurement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Measurement(code = "PCS", stepCode = "LAT")
@RequiredArgsConstructor
public class LATPlanPiecesGenerator extends AbstractMeter {

    @Override
    protected String getMeasurementUnit(String tenant, StepInstance step) {
        return "N";
    }

    @Override
    protected String getMeasurementValue(String tenant, StepInstance step) {
        return String.valueOf((int) step.getMetadata().get("reservationPieces"));
    }
}
