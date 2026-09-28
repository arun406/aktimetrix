package com.aktimetrix.core.meter.impl;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.meter.api.Meter;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.StepInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

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
        return new MeasurementInstance(tenant, code(),
                getMeasurementValue(tenant, step), getMeasurementUnit(tenant, step), step.getProcessInstanceId(),
                step.getId(), stepCode(), Constants.PLAN_MEASUREMENT_TYPE,
                step.getLocationCode(), ZonedDateTime.now());
    }

    protected abstract String getMeasurementUnit(String tenant, StepInstance step);

    protected abstract String getMeasurementValue(String tenant, StepInstance step);

    /**
     * Reads a date-time from the step's metadata, whether it is stored as a {@link LocalDateTime}, a
     * {@link Date} (as read back from MongoDB) or an ISO-8601 string (as read back from JSON).
     *
     * @return the value, or {@code null} when the key is absent
     */
    protected LocalDateTime metadataTime(StepInstance step, String key) {
        return toLocalDateTime(step.getMetadata() == null ? null : step.getMetadata().get(key));
    }

    static LocalDateTime toLocalDateTime(Object value) {
        if (value == null || value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Date) {
            return LocalDateTime.ofInstant(((Date) value).toInstant(), ZoneId.systemDefault());
        }
        return LocalDateTime.parse(value.toString().replace(' ', 'T'));
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
