package com.aktimetrix.core.stereotypes;

import com.aktimetrix.core.api.Constants;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a component, implementing {@code api.PostProcessor}, that runs after a process instance is created and planned, e.g. to notify another system.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface PostProcessor {

    /**
     * Identifies the processor in logs.
     */
    String code();

    /**
     * The process type it applies to, which defaults to the process code; {@code "*"} for every process.
     */
    String processType();

    /**
     * Informational name, logged at startup.
     */
    String name() default "";

    /**
     * Order among the processors of the same process type, lowest first. The built-in publishers use 1000.
     */
    int priority() default 1;

    /**
     * Informational version, logged at startup.
     */
    String version() default Constants.DEFAULT_VERSION;
}
