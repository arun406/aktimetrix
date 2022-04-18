package com.aktimetrix.products.svm.ciq.cdmpc.measurement;

import com.aktimetrix.products.svm.ciq.cdmpc.measurement.util.CDMPCExportStepMeasurementValueCalculator;
import com.aktimetrix.products.svm.core.Constants;
import com.aktimetrix.products.svm.core.model.StepInstance;
import com.aktimetrix.products.svm.core.stereotypes.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Measurement(code = "TIME", stepCode = "LAT")
public class LATPlanTimeGenerator implements Meter {

    private static final Logger logger = LoggerFactory.getLogger(LATPlanTimeGenerator.class);
    private final MeasurementInstanceService measurementInstanceService;

    @Autowired
    private CDMPCExportStepMeasurementValueCalculator valueCalculator;

    /**
     * default constructor
     *
     * @param measurementInstanceService
     */
    public LATPlanTimeGenerator(MeasurementInstanceService measurementInstanceService) {
        this.measurementInstanceService = measurementInstanceService;
    }

    @Override
    public MeasurementInstance measure(String tenant, String measurementCode,
                                       StepInstance bkdStepInstance, StepInstance stepInstance) {

        final String measurementValue = this.valueCalculator.calculate(tenant,
                bkdStepInstance, stepInstance.getStepCode());
        logger.info("Measurement value : " + measurementValue);
        MeasurementInstance measurementInstance = new MeasurementInstance(tenant,
                measurementCode,
                measurementValue,
                "TIMESTAMP",
                bkdStepInstance.getProcessInstanceId(),
                stepInstance.getId(),
                stepInstance.getStepCode(),
                Constants.PLAN_MEASUREMENT_TYPE,
                stepInstance.getLocationCode());

        return measurementInstanceService.saveMeasurementInstance(measurementInstance);
    }
}
