package com.aktimetrix.service.meter.core.meter.impl;

import com.aktimetrix.service.meter.core.api.Constants;
import com.aktimetrix.service.meter.core.meter.api.Meter;
import com.aktimetrix.service.meter.core.model.MeasurementInstance;
import com.aktimetrix.service.meter.core.transferobjects.StepInstanceDTO;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * Base class for all Meters
 *
 * @author arun kumar kandakatla
 */
public abstract class AbstractMeter implements Meter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMeter.class);

    private final com.aktimetrix.service.meter.core.stereotypes.Measurement annotation = getClass().getAnnotation(com.aktimetrix.service.meter.core.stereotypes.Measurement.class);



    /**
     * @param tenant tenant code
     * @param step   step instance
     * @return Measurement instance
     */
    @Override
    public MeasurementInstance measure(String tenant, StepInstanceDTO step) {
        return new MeasurementInstance(tenant, code(),
                getMeasurementValue(tenant, step), getMeasurementUnit(tenant, step), new ObjectId(step.getProcessInstanceId()),
                new ObjectId(step.getId()), stepCode(), Constants.PLAN_MEASUREMENT_TYPE,
                step.getLocationCode(), ZonedDateTime.now());
    }

    protected abstract String getMeasurementUnit(String tenant, StepInstanceDTO step);

    protected abstract String getMeasurementValue(String tenant, StepInstanceDTO step);



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
