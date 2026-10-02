package com.aktimetrix.core.meter.impl;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.meter.api.Meter;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.util.Times;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
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
                step.getLocationCode(), ZonedDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public MeasurementInstance measureActual(String tenant, StepInstance step, Event<?, ?> event) {
        final String value = getActualValue(tenant, step, event);
        if (value == null) {
            return null;
        }
        return new MeasurementInstance(tenant, code(), value, getMeasurementUnit(tenant, step), step.getProcessInstanceId(),
                step.getId(), stepCode(), Constants.ACTUAL_MEASUREMENT_TYPE, step.getLocationCode(), ZonedDateTime.now(ZoneOffset.UTC));
    }

    /**
     * The actual value when the step completes, e.g. read from the event's entity. Override it for an actual
     * ({@code A}) measurement; by default there is none.
     *
     * @param event the event that completed the step
     */
    protected String getActualValue(String tenant, StepInstance step, Event<?, ?> event) {
        return null;
    }

    protected abstract String getMeasurementUnit(String tenant, StepInstance step);

    protected abstract String getMeasurementValue(String tenant, StepInstance step);

    /**
     * Reads a date-time from the step's metadata as a UTC instant, whether it is stored as an {@link Instant}, a
     * {@link java.util.Date} (as read back from some stores) or an ISO-8601 string (as read back from JSON); a
     * date-time without an offset is taken as UTC. See {@link Times#toInstant}.
     *
     * @return the value, or {@code null} when the key is absent
     */
    protected Instant metadataTime(StepInstance step, String key) {
        return Times.toInstant(step.getMetadata() == null ? null : step.getMetadata().get(key));
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
