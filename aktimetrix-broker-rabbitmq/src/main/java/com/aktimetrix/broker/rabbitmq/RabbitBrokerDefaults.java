package com.aktimetrix.broker.rabbitmq;

import com.aktimetrix.autoconfigure.AktimetrixDefaultProperties;
import com.aktimetrix.core.outbox.OutboxRelay;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ binder settings, with the lowest precedence so the application can override any of them.
 * <ul>
 *     <li><b>Order.</b> The events queue has a single active consumer, so its events are processed in the order they
 *     arrive, whichever instance is active; other instances take over if it stops. To process in parallel and still
 *     keep each entity's events in order, use Spring Cloud Stream partitioning, keyed by entity id.</li>
 *     <li><b>Dead letters.</b> Events that still fail after the binder's retries are republished to the dead-letter
 *     exchange, {@code aktimetrix.events.dead-letter.topic}, and kept in a queue of the same name, where invalid
 *     events are sent too.</li>
 *     <li><b>Routing keys.</b> Every published event is routed by the id of the instance it is about (the
 *     {@value OutboxRelay#MESSAGE_KEY_HEADER} header); the outbound exchanges are topic exchanges.</li>
 * </ul>
 */
public class RabbitBrokerDefaults implements EnvironmentPostProcessor {

    static final String SOURCE_NAME = "aktimetrixRabbitDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        final Map<String, Object> defaults = new HashMap<>();
        final String prefix = "spring.cloud.stream.rabbit.bindings.";
        final String deadLetters = AktimetrixDefaultProperties.DEAD_LETTER_TOPIC;
        final String consumer = prefix + "processor-in-0.consumer.";
        defaults.put(consumer + "singleActiveConsumer", "true");
        defaults.put(consumer + "autoBindDlq", "${aktimetrix.events.dead-letter.enabled:true}");
        defaults.put(consumer + "republishToDlq", "true");
        defaults.put(consumer + "deadLetterExchange", deadLetters);
        defaults.put(consumer + "deadLetterExchangeType", "direct");
        defaults.put(consumer + "deadLetterQueueName", deadLetters);
        defaults.put(consumer + "deadLetterRoutingKey", deadLetters);
        for (String binding : AktimetrixDefaultProperties.OUTBOUND_BINDINGS) {
            if (AktimetrixDefaultProperties.DEAD_LETTER_BINDING.equals(binding)) {
                continue;
            }
            defaults.put(prefix + binding + ".producer.routingKeyExpression",
                    "headers['" + OutboxRelay.MESSAGE_KEY_HEADER + "']");
        }
        // invalid events go to the same dead-letter exchange and queue as failed ones
        final String deadLetterProducer = prefix + AktimetrixDefaultProperties.DEAD_LETTER_BINDING + ".producer.";
        defaults.put(deadLetterProducer + "exchangeType", "direct");
        defaults.put(deadLetterProducer + "routingKey", deadLetters);
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, defaults));
    }
}
