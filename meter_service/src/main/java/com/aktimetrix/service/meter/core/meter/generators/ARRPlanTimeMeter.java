package com.aktimetrix.service.meter.core.meter.generators;

import com.aktimetrix.core.meter.impl.AbstractMeter;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.stereotypes.Measurement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Measurement(code = "TIME", stepCode = "ARR")
@RequiredArgsConstructor
public class ARRPlanTimeMeter extends AbstractMeter {

    private final CDMPCImportStepMeasurementValueCalculator valueCalculator;

    @Override
    protected String getMeasurementUnit(String tenant, StepInstance step) {
        return "TIMESTAMP";
    }

    @Override
    protected String getMeasurementValue(String tenant, StepInstance step) {
        return this.valueCalculator.calculate(tenant, step, stepCode());
    }
}
