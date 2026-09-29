package com.aktimetrix.it.orders;

import com.aktimetrix.core.meter.impl.AbstractMeter;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.stereotypes.Measurement;
import org.springframework.stereotype.Component;

/**
 * The planning rule: a priority customer's order is delivered within 4 hours of being created, others within 2 days.
 */
@Component
@Measurement(code = "TIME", stepCode = "DELIVERED")
public class DeliveryPlanMeter extends AbstractMeter {

    @Override
    protected String getMeasurementUnit(String tenant, StepInstance step) {
        return "TIMESTAMP";
    }

    @Override
    protected String getMeasurementValue(String tenant, StepInstance step) {
        boolean priority = Boolean.TRUE.equals(step.getMetadata().get("priority"));
        return String.valueOf(priority
                ? metadataTime(step, "createdAt").plusHours(4)
                : metadataTime(step, "createdAt").plusDays(2));
    }
}
