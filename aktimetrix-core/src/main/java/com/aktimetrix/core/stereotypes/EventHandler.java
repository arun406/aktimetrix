package com.aktimetrix.core.stereotypes;


import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.EventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface EventHandler {
    String eventType();

    String name() default "";

    String version() default Constants.DEFAULT_VERSION;
}
