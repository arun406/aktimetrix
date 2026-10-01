package com.aktimetrix.broker.kafka;

import com.aktimetrix.autoconfigure.AktimetrixDefaultProperties;
import com.aktimetrix.core.outbox.OutboxRelay;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka binder settings, with the lowest precedence so the application can override any of them:
 * <ul>
 *     <li>events that still fail after the binder's retries go to the dead-letter topic,
 *     {@code aktimetrix.events.dead-letter.topic}, the same topic invalid events are sent to;</li>
 *     <li>every published event is keyed by the id of the instance it is about (the {@value OutboxRelay#MESSAGE_KEY_HEADER}
 *     header), as a string, so the events of one instance stay in order.</li>
 * </ul>
 */
public class KafkaBrokerDefaults implements EnvironmentPostProcessor {

    static final String SOURCE_NAME = "aktimetrixKafkaDefaults";
    private static final String STRING_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        final Map<String, Object> defaults = new HashMap<>();
        final String prefix = "spring.cloud.stream.kafka.bindings.";
        defaults.put(prefix + "processor-in-0.consumer.enableDlq", "${aktimetrix.events.dead-letter.enabled:true}");
        defaults.put(prefix + "processor-in-0.consumer.dlqName", AktimetrixDefaultProperties.DEAD_LETTER_TOPIC);
        for (String binding : AktimetrixDefaultProperties.OUTBOUND_BINDINGS) {
            defaults.put(prefix + binding + ".producer.messageKeyExpression",
                    "headers['" + OutboxRelay.MESSAGE_KEY_HEADER + "']");
            defaults.put(prefix + binding + ".producer.configuration[key.serializer]", STRING_SERIALIZER);
        }
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, defaults));
    }
}
