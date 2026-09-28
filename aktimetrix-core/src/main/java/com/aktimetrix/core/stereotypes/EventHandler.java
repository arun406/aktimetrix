package com.aktimetrix.core.stereotypes;


import com.aktimetrix.core.api.Constants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a component that handles one event code instead of the default handler, e.g. to read the entity id or the
 * business time differently. Extend {@code AbstractEventHandler} for events that may start processes, or
 * {@code AbstractMilestoneEventHandler} for events that only record milestones. At most one per event code.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface EventHandler {
    /**
     * The event code handled, e.g. {@code ORDER_SHIPPED_EVENT}.
     */
    String eventType();

    /**
     * Informational name, logged at startup.
     */
    String name() default "";

    /**
     * Informational version, logged at startup.
     */
    String version() default Constants.DEFAULT_VERSION;
}
