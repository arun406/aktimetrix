package com.aktimetrix.it;

import com.aktimetrix.core.meter.impl.AbstractMeter;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.stereotypes.Measurement;
import org.springframework.stereotype.Component;

/**
 * A parcel should be picked up within an hour of being booked.
 */
@Component
@Measurement(code = "TIME", stepCode = "PICKUP")
public class PickupPlanMeter extends AbstractMeter {

    @Override
    protected String getMeasurementUnit(String tenant, StepInstance step) {
        return "TIMESTAMP";
    }

    @Override
    protected String getMeasurementValue(String tenant, StepInstance step) {
        return String.valueOf(metadataTime(step, "bookedAt").plusHours(1));
    }
}
