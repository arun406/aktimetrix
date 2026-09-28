package com.aktimetrix.it.orders;

import com.aktimetrix.core.meter.impl.AbstractProcessMeter;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.stereotypes.Measurement;
import org.springframework.stereotype.Component;

/**
 * The planning rule for the whole order: a priority customer's order is delivered within 1 day, others within 3.
 */
@Component
@Measurement(code = "TIME", processCode = "ORDER_DELIVERY")
public class OrderDeadlineMeter extends AbstractProcessMeter {

    @Override
    protected String getMeasurementUnit(String tenant, ProcessInstance process) {
        return "TIMESTAMP";
    }

    @Override
    protected String getMeasurementValue(String tenant, ProcessInstance process) {
        boolean priority = Boolean.TRUE.equals(process.getMetadata().get("priority"));
        return String.valueOf(metadataTime(process, "createdAt").plusDays(priority ? 1 : 3));
    }
}
