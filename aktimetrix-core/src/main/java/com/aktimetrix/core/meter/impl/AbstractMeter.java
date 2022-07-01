package com.aktimetrix.core.meter.impl;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.meter.api.Meter;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.StepInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all Meters
 *
 * @author arun kumar kandakatla
 */
public abstract class AbstractMeter implements Meter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMeter.class);

    private final com.aktimetrix.core.stereotypes.Measurement annotation = getClass()
            .getAnnotation(com.aktimetrix.core.stereotypes.Measurement.class);


    /**
     * @param tenant tenant code
     * @param step   step instance
     * @return Measurement instance
     */
    @Override
    public MeasurementInstance measure(String tenant, StepInstance step) {
        MeasurementInstance measurementInstance = new MeasurementInstance(tenant, code(),
                getMeasurementValue(tenant, step), getMeasurementUnit(tenant, step), step.getProcessInstanceId(),
                step.getId(), stepCode(), Constants.PLAN_MEASUREMENT_TYPE,
                step.getLocationCode(), ZonedDateTime.now());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentType", step.getMetadata().get("documentType"));
        metadata.put("documentNumber", step.getMetadata().get("documentNumber"));
        measurementInstance.setMetadata(metadata);
        return measurementInstance;
    }

    protected abstract String getMeasurementUnit(String tenant, StepInstance step);

    protected abstract String getMeasurementValue(String tenant, StepInstance step);


    /**
     * returns the annotation name;
     *
     * @return name
     */
    public String name() {
        return this.annotation.name();
    }

    /**
     * returns the versions
     *
     * @return version
     */
    public String version() {
        return this.annotation.version();
    }

    /**
     * returns the code
     *
     * @return code
     */
    public String code() {
        return this.annotation.code();
    }

    /**
     * returns the step code
     *
     * @return step code
     */
    public String stepCode() {
        return this.annotation.stepCode();
    }

    @Override
    public String toString() {
        return "AbstractMeter {" +
                "annotation=" + annotation +
                '}';
    }
}
