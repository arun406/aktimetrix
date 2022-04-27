package com.aktimetrix.products.svm.ciq.cdmpc.measurement.util;

import com.aktimetrix.products.svm.core.model.StepInstance;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CDMP-C Export Milestone Plan Time  Generator
 */
@Component
public class CDMPCImportStepMeasurementValueCalculator {

    private static final Logger logger = LoggerFactory.getLogger(CDMPCImportStepMeasurementValueCalculator.class);
    private OffsetCalculator offsetCalculator;

    /**
     * default constructor
     *
     * @param offsetCalculator
     */
    public CDMPCImportStepMeasurementValueCalculator(OffsetCalculator offsetCalculator) {
        this.offsetCalculator = offsetCalculator;
    }

    /**
     * prepares the measurement instance
     *
     * @param tenant
     * @param bkdStepInstance
     * @param stepCode
     * @return
     */
    public String calculate(String tenant, StepInstance bkdStepInstance,
                            String stepCode) {
        final Document metadata = bkdStepInstance.getMetadata();

        long offset = this.offsetCalculator.getOffset(tenant, bkdStepInstance, metadata, stepCode);
        logger.info(String.format("%s Step Code offset in minutes is :  %s", stepCode, offset));

        // ARR plan time is STA + offset
        final LocalDateTime planTime = ((LocalDateTime) metadata.get("sta")).plusHours(offset);
        final String planTimeStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(planTime);
        logger.info("  Plan Time: " + planTimeStr);
        return planTimeStr;

    }

}
