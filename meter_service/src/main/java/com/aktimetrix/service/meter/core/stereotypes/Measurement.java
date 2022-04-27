package com.aktimetrix.service.meter.core.stereotypes;

import org.springframework.stereotype.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Service
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Measurement {

    String name() default "";

    String code();

    String stepCode();

    String version() default "1.0.0";
}
