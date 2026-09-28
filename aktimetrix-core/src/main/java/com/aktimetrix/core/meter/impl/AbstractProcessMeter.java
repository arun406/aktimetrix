package com.aktimetrix.core.meter.impl;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.meter.api.ProcessMeter;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.transferobjects.Event;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * Base class for process-level meters, annotated with {@code @Measurement(code = "…", processCode = "…")}.
 * Called once when a process instance is created; the measurement belongs to the process, not to a step.
 */
public abstract class AbstractProcessMeter implements ProcessMeter {

    private final com.aktimetrix.core.stereotypes.Measurement annotation = getClass()
            .getAnnotation(com.aktimetrix.core.stereotypes.Measurement.class);

    @Override
    public MeasurementInstance measure(String tenant, ProcessInstance process) {
        return new MeasurementInstance(tenant, code(), getMeasurementValue(tenant, process),
                getMeasurementUnit(tenant, process), process.getId(), null, null, Constants.PLAN_MEASUREMENT_TYPE,
                null, ZonedDateTime.now());
    }

    @Override
    public MeasurementInstance measureActual(String tenant, ProcessInstance process, Event<?, ?> event) {
        final String value = getActualValue(tenant, process, event);
        if (value == null) {
            return null;
        }
        return new MeasurementInstance(tenant, code(), value, getMeasurementUnit(tenant, process), process.getId(),
                null, null, Constants.ACTUAL_MEASUREMENT_TYPE, null, ZonedDateTime.now());
    }

    /**
     * The actual value when the process completes, e.g. read from the event's entity. Override it for an actual
     * ({@code A}) measurement; by default there is none.
     *
     * @param event the event that completed the process
     */
    protected String getActualValue(String tenant, ProcessInstance process, Event<?, ?> event) {
        return null;
    }

    protected abstract String getMeasurementUnit(String tenant, ProcessInstance process);

    protected abstract String getMeasurementValue(String tenant, ProcessInstance process);

    /**
     * Reads a date-time from the process's metadata, as {@link AbstractMeter#metadataTime} does for a step.
     *
     * @return the value, or {@code null} when the key is absent
     */
    protected LocalDateTime metadataTime(ProcessInstance process, String key) {
        return AbstractMeter.toLocalDateTime(process.getMetadata() == null ? null : process.getMetadata().get(key));
    }

    public String code() {
        return this.annotation.code();
    }

    public String processCode() {
        return this.annotation.processCode();
    }
}
