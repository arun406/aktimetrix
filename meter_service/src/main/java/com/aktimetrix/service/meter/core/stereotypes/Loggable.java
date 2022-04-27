package com.aktimetrix.service.meter.core.stereotypes;


import org.slf4j.event.Level;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = {METHOD, TYPE})
public @interface Loggable {

    Level value() default Level.INFO;

    ChronoUnit unit() default ChronoUnit.SECONDS;

    boolean showArgs() default false;

    boolean showResult() default false;

    boolean showExecutionTime() default true;
}
