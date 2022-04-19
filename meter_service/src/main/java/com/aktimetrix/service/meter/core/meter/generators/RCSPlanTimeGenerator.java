package com.aktimetrix.service.meter.core.meter.generators;

import com.aktimetrix.service.meter.core.meter.impl.AbstractMeter;
import com.aktimetrix.service.meter.core.stereotypes.Measurement;
import com.aktimetrix.service.meter.core.transferobjects.StepInstanceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Measurement(code = "TIME", stepCode = "RCS")
public class RCSPlanTimeGenerator extends AbstractMeter {
    private static final Logger logger = LoggerFactory.getLogger(RCSPlanTimeGenerator.class);

    @Autowired
    private CDMPCExportStepMeasurementValueCalculator valueCalculator;

    @Override
    protected String getMeasurementUnit(String tenant, StepInstanceDTO step) {
        return this.valueCalculator.calculate(tenant, step, stepCode());
    }

    @Override
    protected String getMeasurementValue(String tenant, StepInstanceDTO step) {
        return "TIMESTAMP";
    }
}
