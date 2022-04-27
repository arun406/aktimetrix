package com.aktimetrix.service.meter.core.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DateTimeUtil {
    /**
     * Returns
     *
     * @param value
     * @return
     */
    public static LocalDateTime getLocalDateTime(String value) {
        LocalDateTime newPlanTime;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        newPlanTime = LocalDateTime.parse(value, formatter);
        return newPlanTime;
    }

    public static LocalDateTime getLocalDateTime(String value, DateTimeFormatter formatter) {
        LocalDateTime newPlanTime;
        newPlanTime = LocalDateTime.parse(value, formatter);
        return newPlanTime;
    }

    public static LocalDateTime getLocalDateTime(String value, String format) {
        LocalDateTime newPlanTime;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        newPlanTime = LocalDateTime.parse(value, formatter);
        return newPlanTime;
    }
}
