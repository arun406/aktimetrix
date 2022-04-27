package com.aktimetrix.service.meter.core.meter.generators;

import com.aktimetrix.core.meter.impl.AbstractMeter;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.stereotypes.Measurement;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Measurement(code = "TIME", stepCode = "LAT")
@RequiredArgsConstructor
public class LATPlanTimeMeter extends AbstractMeter {

    private static final Logger logger = LoggerFactory.getLogger(LATPlanTimeMeter.class);
    private final CDMPCExportStepMeasurementValueCalculator valueCalculator;

    @Override
    protected String getMeasurementUnit(String tenant, StepInstance step) {
        return "TIMESTAMP";
    }

    @Override
    protected String getMeasurementValue(String tenant, StepInstance step) {
        return this.valueCalculator.calculate(tenant, step, stepCode());
    }
}
