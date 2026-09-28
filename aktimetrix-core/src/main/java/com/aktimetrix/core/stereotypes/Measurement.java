package com.aktimetrix.core.stereotypes;

import com.aktimetrix.core.api.Constants;
import org.springframework.stereotype.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a meter: the component that computes a planned measurement.
 * <p>
 * Set exactly one of {@link #stepCode()} and {@link #processCode()}. A step-level meter extends
 * {@code AbstractMeter} and is called for each new instance of that step; a process-level meter extends
 * {@code AbstractProcessMeter} and is called once for each new instance of that process.
 */
@Service
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Measurement {

    /**
     * Informational name.
     */
    String name() default "";

    /**
     * The measurement computed, e.g. {@code TIME} or {@code DISTANCE}; must match a measurement declared on the step or
     * process definition.
     */
    String code();

    /**
     * The step whose instances this meter measures. Leave empty for a process-level meter.
     */
    String stepCode() default "";

    /**
     * The process whose instances this meter measures. Leave empty for a step-level meter.
     */
    String processCode() default "";

    /**
     * Informational version.
     */
    String version() default Constants.DEFAULT_VERSION;
}
