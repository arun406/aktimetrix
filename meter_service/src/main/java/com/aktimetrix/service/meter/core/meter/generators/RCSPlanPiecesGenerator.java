package com.aktimetrix.service.meter.core.meter.generators;

import com.aktimetrix.service.meter.core.model.MeasurementInstance;
import com.aktimetrix.service.meter.core.stereotypes.Measurement;
import com.aktimetrix.service.meter.core.api.Constants;
import com.aktimetrix.service.meter.core.transferobjects.Step;
import com.aktimetrix.service.meter.core.meter.impl.AbstractMeter;
import com.aktimetrix.service.meter.core.meter.service.MeasurementInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Component
@Measurement(code = "PCS", stepCode = "RCS")
@Slf4j
@RequiredArgsConstructor
public class RCSPlanPiecesGenerator extends AbstractMeter {
    private final MeasurementInstanceService measurementInstanceService;

    @Override
    public com.aktimetrix.service.meter.core.transferobjects.Measurement measure(String tenant, Step step) {
        MeasurementInstance mi = new MeasurementInstance(tenant, code(),
                String.valueOf((int) step.getMetadata().get("reservationPieces")), "N",
                new ObjectId(step.getProcessInstanceId()), new ObjectId(step.getId()), stepCode(),
                Constants.PLAN_MEASUREMENT_TYPE, step.getLocationCode(), ZonedDateTime.now());
        log.debug(" service {}", this.measurementInstanceService);

        this.measurementInstanceService.saveMeasurementInstance(mi);
        return getMeasurement(mi);
    }
}
