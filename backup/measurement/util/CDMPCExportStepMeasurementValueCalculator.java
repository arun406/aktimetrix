package com.aktimetrix.products.svm.ciq.cdmpc.measurement.util;

import com.aktimetrix.products.svm.core.model.StepInstance;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 */
@Component
public class CDMPCExportStepMeasurementValueCalculator {

    private static final Logger logger = LoggerFactory.getLogger(CDMPCExportStepMeasurementValueCalculator.class);
    private final OffsetCalculator offsetCalculator;

    /**
     * @param offsetCalculator
     */
    public CDMPCExportStepMeasurementValueCalculator(OffsetCalculator offsetCalculator) {
        this.offsetCalculator = offsetCalculator;
    }

    /**
     * @param tenant
     * @param bkdStepInstance
     * @param stepCode
     * @return
     */
    public String calculate(String tenant, StepInstance bkdStepInstance, String stepCode) {
        final Document metadata = bkdStepInstance.getMetadata();

        long offset = this.offsetCalculator.getOffset(tenant, bkdStepInstance, metadata, stepCode);
        logger.info(String.format("%s Step Code offset in minutes is :  %s", stepCode, offset));
        LocalDateTime planTime = null;
        if (!"DEP".equalsIgnoreCase(stepCode) && !"DEP-T".equalsIgnoreCase(stepCode)) {
            //  plan time is STD - offset
            planTime = ((LocalDateTime) metadata.get("std")).minusHours(offset);
        } else {
            //  plan time is STD + offset
            planTime = ((LocalDateTime) metadata.get("std")).plusHours(offset);
        }
        final String planTimeStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(planTime);
        logger.info("Plan Time: " + planTimeStr);
        return planTimeStr;
    }

}
