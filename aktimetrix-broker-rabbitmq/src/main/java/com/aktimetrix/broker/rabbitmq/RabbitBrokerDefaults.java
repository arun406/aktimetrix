package com.aktimetrix.broker.rabbitmq;

import com.aktimetrix.autoconfigure.AktimetrixDefaultProperties;
import com.aktimetrix.core.outbox.OutboxRelay;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ binder settings, with the lowest precedence so the application can override any of them.
 * <ul>
 *     <li><b>Order.</b> The events queue has a single active consumer, so its events are processed in the order they
 *     arrive, whichever instance is active; another instance takes over if it stops.</li>
 *     <li><b>Partitions.</b> With {@code aktimetrix.events.partitions} above 1, the events are split by entity into
 *     that many queues, {@code topic.group-N}, bound with the routing key {@code topic-N}; each instance consumes
 *     the partition {@code aktimetrix.events.partition}, with a single active consumer, so instances process in
 *     parallel and each entity's events stay in order. Source systems publish with the routing key from
 *     {@link RabbitEventPartitions#routingKey}.</li>
 *     <li><b>Dead letters.</b> Events that still fail after the binder's retries are republished to the dead-letter
 *     exchange, {@code aktimetrix.events.dead-letter.topic}, and kept in the queue of the same name, where invalid
 *     events go too. The exchange and queue are declared by {@link RabbitDeadLetterAutoConfiguration}, with plain
 *     AMQP, rather than through RabbitMQ's dead-letter queue arguments.</li>
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
        final Integer partitions = environment.getProperty("aktimetrix.events.partitions", Integer.class, 1);
        if (partitions > 1) {
            final String binding = "spring.cloud.stream.bindings.processor-in-0.consumer.";
            defaults.put(binding + "partitioned", "true");
            defaults.put(binding + "instanceCount", String.valueOf(partitions));
            defaults.put(binding + "instanceIndex", "${aktimetrix.events.partition}");
        }
        defaults.put(consumer + "republishToDlq", "${aktimetrix.events.dead-letter.enabled:true}");
        defaults.put(consumer + "deadLetterExchange", deadLetters);
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
        defaults.put(deadLetterProducer + "routingKeyExpression", "'" + deadLetters + "'");
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, defaults));
    }
}
