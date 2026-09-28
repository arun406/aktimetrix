package com.aktimetrix.core.stereotypes;

import com.aktimetrix.core.api.Constants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the component that creates the instances of one process, usually by extending {@code AbstractProcessor} to
 * choose the metadata of the process and its steps. Processes without one use {@code DefaultProcessor}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ProcessHandler {
    /**
     * The process code handled, e.g. {@code ORDER_DELIVERY}.
     */
    String processType();

    /**
     * Informational name, logged at startup.
     */
    String name() default "";

    /**
     * Informational version, logged at startup.
     */
    String version() default Constants.DEFAULT_VERSION;
}
