package com.aktimetrix.service.meter.core.meter.impl;

import com.aktimetrix.service.meter.core.model.MeasurementInstance;
import com.aktimetrix.service.meter.core.meter.api.Meter;
import com.aktimetrix.service.meter.core.transferobjects.Measurement;

/**
 * Base class for all Meters
 *
 * @author arun kumar kandakatla
 */
public abstract class AbstractMeter implements Meter {

    private final com.aktimetrix.service.meter.core.stereotypes.Measurement annotation;

    public AbstractMeter() {
        annotation = getClass().getAnnotation(com.aktimetrix.service.meter.core.stereotypes.Measurement.class);
    }


    public com.aktimetrix.service.meter.core.transferobjects.Measurement getMeasurement(MeasurementInstance mi) {
        return Measurement.builder()
                .type(mi.getType())
                .tenant(mi.getTenant())
                .code(mi.getCode())
                .id(mi.getId().toString())
                .measuredAt(mi.getMeasuredAt())
                .createdOn(mi.getCreatedOn())
                .stepCode(mi.getStepCode())
                .processInstanceId(mi.getProcessInstanceId().toString())
                .stepInstanceId(mi.getStepInstanceId().toString())
                .unit(mi.getUnit())
                .value(mi.getValue())
                .build();
    }

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
