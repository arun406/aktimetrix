package com.aktimetrix.service.meter.core.meter.generators;

import com.aktimetrix.service.meter.core.meter.impl.AbstractMeter;
import com.aktimetrix.service.meter.core.stereotypes.Measurement;
import com.aktimetrix.service.meter.core.transferobjects.StepInstanceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Measurement(code = "TIME", stepCode = "ARR")
@RequiredArgsConstructor
public class ARRPlanTimeGenerator extends AbstractMeter {

    private final CDMPCImportStepMeasurementValueCalculator valueCalculator;

    @Override
    protected String getMeasurementUnit(String tenant, StepInstanceDTO step) {
        return "TIMESTAMP";
    }

    @Override
    protected String getMeasurementValue(String tenant, StepInstanceDTO step) {
        return this.valueCalculator.calculate(tenant, step, stepCode());
    }
}
