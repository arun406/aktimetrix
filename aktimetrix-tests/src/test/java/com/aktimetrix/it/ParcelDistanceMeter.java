package com.aktimetrix.it;

import com.aktimetrix.core.meter.impl.AbstractProcessMeter;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.stereotypes.Measurement;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The planned distance of the whole parcel journey: a process-level measurement.
 */
@Component
@Measurement(code = "DISTANCE", processCode = "PARCEL")
public class ParcelDistanceMeter extends AbstractProcessMeter {

    private static final Map<String, Integer> ROUTES_KM = Map.of("AMS-RTM", 78);

    @Override
    protected String getMeasurementUnit(String tenant, ProcessInstance process) {
        return "KM";
    }

    @Override
    protected String getMeasurementValue(String tenant, ProcessInstance process) {
        String route = process.getMetadata().get("from") + "-" + process.getMetadata().get("to");
        return String.valueOf(ROUTES_KM.get(route));
    }
}
