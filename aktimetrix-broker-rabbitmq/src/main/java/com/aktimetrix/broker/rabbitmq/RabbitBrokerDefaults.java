package com.aktimetrix.broker.rabbitmq;

import com.aktimetrix.autoconfigure.AktimetrixDefaultProperties;
import com.aktimetrix.core.configurations.ProcessConfig;
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
 *     <li><b>Partitions.</b> With {@code aktimetrix.events.partitions} above 1, an event router consumes the events
 *     queue, in order, and republishes each event to the partition of its entity, on the exchange
 *     {@code topic.partitioned}; each instance processes the partition {@code aktimetrix.events.partition}, with a
 *     single active consumer, so instances process in parallel and each entity's events stay in order. Source
 *     systems publish as before: the router reads the entity id with the event mapper.</li>
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

    /**
     * The internal exchange the event router publishes to, partition by partition.
     */
    static final String PARTITIONED_TOPIC = "${aktimetrix.events.topic:business-events}.partitioned";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        final Map<String, Object> defaults = new HashMap<>();
        final String prefix = "spring.cloud.stream.rabbit.bindings.";
        final String deadLetters = AktimetrixDefaultProperties.DEAD_LETTER_TOPIC;
        consumeInOrder(defaults, prefix + "processor-in-0.consumer.", deadLetters);
        final Integer partitions = environment.getProperty("aktimetrix.events.partitions", Integer.class, 1);
        if (partitions > 1) {
            partition(defaults, prefix, deadLetters, partitions);
        }
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
        final MapPropertySource source = new MapPropertySource(SOURCE_NAME, defaults);
        // these take precedence over the broker-neutral defaults, whichever was added first
        if (environment.getPropertySources().contains(AktimetrixDefaultProperties.SOURCE_NAME)) {
            environment.getPropertySources().addBefore(AktimetrixDefaultProperties.SOURCE_NAME, source);
        } else {
            environment.getPropertySources().addLast(source);
        }
    }

    /**
     * One active consumer at a time, so a queue is processed in order; failures go to the dead-letter exchange.
     */
    private static void consumeInOrder(Map<String, Object> defaults, String consumer, String deadLetters) {
        defaults.put(consumer + "singleActiveConsumer", "true");
        defaults.put(consumer + "republishToDlq", "${aktimetrix.events.dead-letter.enabled:true}");
        defaults.put(consumer + "deadLetterExchange", deadLetters);
        defaults.put(consumer + "deadLetterRoutingKey", deadLetters);
    }

    /**
     * The event router consumes the events queue, in order, and republishes each event to the partition of its entity
     * on {@value #PARTITIONED_TOPIC}, in the same AMQP transaction as it acknowledges it; the processor of each instance
     * consumes its own partition.
     */
    private static void partition(Map<String, Object> defaults, String prefix, String deadLetters, int partitions) {
        final String bindings = "spring.cloud.stream.bindings.";
        defaults.put("spring.cloud.function.definition", "eventRouter;processor");
        defaults.put(bindings + "eventRouter-in-0.destination", "${aktimetrix.events.topic:business-events}");
        defaults.put(bindings + "eventRouter-in-0.group", "${aktimetrix.events.group:aktimetrix}");
        defaults.put(bindings + "eventRouter-out-0.destination", PARTITIONED_TOPIC);
        defaults.put(bindings + "eventRouter-out-0.producer.partitionKeyExpression",
                "headers['" + ProcessConfig.PARTITION_KEY_HEADER + "']");
        defaults.put(bindings + "eventRouter-out-0.producer.partitionCount", String.valueOf(partitions));
        // every partition's queue exists before the first event is routed, whichever instances are running
        defaults.put(bindings + "eventRouter-out-0.producer.requiredGroups", "${aktimetrix.events.group:aktimetrix}");
        consumeInOrder(defaults, prefix + "eventRouter-in-0.consumer.", deadLetters);
        defaults.put(prefix + "eventRouter-in-0.consumer.transacted", "true");
        defaults.put(prefix + "eventRouter-out-0.producer.transacted", "true");

        defaults.put(bindings + "processor-in-0.destination", PARTITIONED_TOPIC);
        defaults.put(bindings + "processor-in-0.consumer.partitioned", "true");
        defaults.put(bindings + "processor-in-0.consumer.instanceCount", String.valueOf(partitions));
        defaults.put(bindings + "processor-in-0.consumer.instanceIndex", "${aktimetrix.events.partition}");
    }
}
