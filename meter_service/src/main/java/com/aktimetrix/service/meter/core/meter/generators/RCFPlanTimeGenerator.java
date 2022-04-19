package com.aktimetrix.service.meter.core.meter.generators;

import com.aktimetrix.service.meter.core.meter.impl.AbstractMeter;
import com.aktimetrix.service.meter.core.stereotypes.Measurement;
import com.aktimetrix.service.meter.core.transferobjects.StepInstanceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Measurement(code = "TIME", stepCode = "RCF")
public class RCFPlanTimeGenerator extends AbstractMeter {

    private static final Logger logger = LoggerFactory.getLogger(RCFPlanTimeGenerator.class);

    @Autowired
    private CDMPCImportStepMeasurementValueCalculator valueCalculator;

    @Override
    protected String getMeasurementUnit(String tenant, StepInstanceDTO step) {
        final String measurementValue = this.valueCalculator.calculate(tenant, step, stepCode());
        logger.info("Measurement value : {}", measurementValue);
        return measurementValue;
    }

    @Override
    protected String getMeasurementValue(String tenant, StepInstanceDTO step) {
        return "TIMESTAMP";
    }
}
