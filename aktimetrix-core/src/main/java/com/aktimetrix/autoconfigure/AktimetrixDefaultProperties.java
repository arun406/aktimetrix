package com.aktimetrix.autoconfigure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Supplies the Spring Cloud Stream and Jackson settings Aktimetrix needs, with the lowest precedence, so an
 * application only has to point at its Kafka broker and MongoDB. Any of them can be overridden in the application's
 * own configuration.
 */
public class AktimetrixDefaultProperties implements EnvironmentPostProcessor {

    static final String SOURCE_NAME = "aktimetrixDefaults";
    private static final String STRING_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new HashMap<>();
        // inbound business events
        defaults.put("spring.cloud.stream.function.definition", "processor");
        defaults.put("spring.cloud.stream.bindings.processor-in-0.destination", "${aktimetrix.events.topic:business-events}");
        defaults.put("spring.cloud.stream.bindings.processor-in-0.group", "${aktimetrix.events.group:aktimetrix}");
        // outbound instance events are keyed by instance id
        for (String binding : new String[]{"process-instance-out-0", "step-instance-out-0", "measurement-instance-out-0"}) {
            defaults.put("spring.cloud.stream.kafka.bindings." + binding + ".producer.configuration[key.serializer]",
                    STRING_SERIALIZER);
        }
        defaults.put("spring.jackson.serialization.write-dates-as-timestamps", "false");
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, defaults));
    }
}
