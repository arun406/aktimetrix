package com.aktimetrix.service.meter.core.meter.generators;


import com.aktimetrix.service.meter.core.model.MeasurementInstance;
import com.aktimetrix.service.meter.core.stereotypes.Measurement;
import com.aktimetrix.service.meter.core.api.Constants;
import com.aktimetrix.service.meter.core.transferobjects.Step;
import com.aktimetrix.service.meter.core.meter.impl.AbstractMeter;
import com.aktimetrix.service.meter.core.meter.service.MeasurementInstanceService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Component
@Measurement(code = "VOL", stepCode = "RCS")
@RequiredArgsConstructor
public class RCSPlanVolumeGenerator extends AbstractMeter {

    private final MeasurementInstanceService measurementInstanceService;

    @Override
    public com.aktimetrix.service.meter.core.transferobjects.Measurement measure(String tenant, Step step) {
        MeasurementInstance mi = new MeasurementInstance(tenant, code(),
                String.valueOf((double) step.getMetadata().get("reservationVolume")), (String) step.getMetadata().get("reservationVolumeUnit"),
                new ObjectId(step.getProcessInstanceId()), new ObjectId(step.getId()), stepCode(),
                Constants.PLAN_MEASUREMENT_TYPE, step.getLocationCode(), ZonedDateTime.now());

        this.measurementInstanceService.saveMeasurementInstance(mi);
        return getMeasurement(mi);
    }
}

