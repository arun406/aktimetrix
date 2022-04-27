package com.aktimetrix.service.meter.core.meter.generators;

import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * CDMP-C Export Milestone Plan Time  Generator
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CDMPCImportStepMeasurementValueCalculator {

    private final OffsetCalculator offsetCalculator;

    /**
     * prepares the measurement instance
     *
     * @param tenant   tenant
     * @param step     step
     * @param stepCode step code
     * @return measurement value
     */
    public String calculate(String tenant, StepInstance step, String stepCode) {
        final Map<String, Object> metadata = step.getMetadata();

        long offset = this.offsetCalculator.getOffset(tenant, metadata, stepCode);
        log.info(String.format("%s Step Code offset in minutes is :  %s", stepCode, offset));

        // ARR plan time is STA + offset
        final LocalDateTime sta = DateTimeUtil.getLocalDateTime((String) metadata.get("sta"), "yyyy-MM-dd'T'HH:mm:ss");
        final LocalDateTime planTime = sta.plusMinutes(offset);
        final String planTimeStr = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").format(planTime);
        log.info("measurement : {} ", planTimeStr);
        return planTimeStr;
    }

}
