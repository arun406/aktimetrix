package com.aktimetrix.autoconfigure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Supplies the broker-neutral Spring Cloud Stream and Jackson settings Aktimetrix needs, with the lowest precedence, so
 * an application only has to point at its broker and state store. Any of them can be overridden in the application's
 * own configuration. Each broker module adds the defaults specific to its binder, such as dead-lettering and message
 * keys.
 */
public class AktimetrixDefaultProperties implements EnvironmentPostProcessor {

    /**
     * Name of the property source of these defaults; a broker module's own defaults take precedence over it.
     */
    public static final String SOURCE_NAME = "aktimetrixDefaults";
    /**
     * Binding of the dead-letter channel, for events Aktimetrix rejects itself.
     */
    public static final String DEAD_LETTER_BINDING = "dead-letter-out-0";
    /**
     * The outbound bindings: results, and dead letters.
     */
    public static final String[] OUTBOUND_BINDINGS = {"process-instance-out-0", "step-instance-out-0",
            "measurement-instance-out-0", DEAD_LETTER_BINDING};
    /**
     * Name of the dead-letter destination: {@code aktimetrix.events.dead-letter.topic}, by default the events topic
     * followed by {@code .dlq}.
     */
    public static final String DEAD_LETTER_TOPIC =
            "${aktimetrix.events.dead-letter.topic:${aktimetrix.events.topic:business-events}.dlq}";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new HashMap<>();
        // inbound business events
        defaults.put("spring.cloud.function.definition", "processor");
        defaults.put("spring.cloud.stream.bindings.processor-in-0.destination", "${aktimetrix.events.topic:business-events}");
        defaults.put("spring.cloud.stream.bindings.processor-in-0.group", "${aktimetrix.events.group:aktimetrix}");
        // invalid events go to the dead-letter channel through the outbox and the dead-letter-out-0 binding; events
        // that still fail after retries are dead-lettered by the binder, as configured by the broker module
        defaults.put("spring.cloud.stream.bindings." + DEAD_LETTER_BINDING + ".destination", DEAD_LETTER_TOPIC);
        // dates are published as ISO-8601 text: the default of Jackson 3, so nothing to set
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, defaults));
    }
}
