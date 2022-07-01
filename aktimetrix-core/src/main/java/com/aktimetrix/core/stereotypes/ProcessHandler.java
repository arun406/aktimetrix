package com.aktimetrix.core.stereotypes;

import com.aktimetrix.core.api.Constants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ProcessHandler {
    String processType() default "";

    String processCode();

    String name() default "";

    String version() default Constants.DEFAULT_VERSION;
}
