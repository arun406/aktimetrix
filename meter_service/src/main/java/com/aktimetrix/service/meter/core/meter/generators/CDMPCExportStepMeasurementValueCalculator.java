package com.aktimetrix.service.meter.core.meter.generators;

import com.aktimetrix.service.meter.core.transferobjects.Step;
import com.aktimetrix.service.meter.core.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CDMPCExportStepMeasurementValueCalculator {

    private final OffsetCalculator offsetCalculator;

    /**
     * @param tenant   tenant code
     * @param step     step instance of BKD
     * @param stepCode step code
     * @return measurement value
     */
    public String calculate(String tenant, Step step, String stepCode) {
        final Map<String, Object> metadata = step.getMetadata();

        long offset = this.offsetCalculator.getOffset(tenant, metadata, stepCode);
        log.info(String.format("%s Step Code offset in minutes is :  %s", stepCode, offset));
        LocalDateTime planTime;
        final LocalDateTime std = DateTimeUtil.getLocalDateTime((String) metadata.get("std"), "yyyy-MM-dd HH:mm");
        //  plan time is STD - offset
        //  plan time is STD + offset
        planTime = !"DEP".equalsIgnoreCase(stepCode) && !"DEP-T".equalsIgnoreCase(stepCode) ? std.minusMinutes(offset) : std.plusMinutes(offset);
        final String planTimeStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(planTime);
        log.info("Plan Time: {}", planTimeStr);
        return planTimeStr;
    }
}
