package com.aktimetrix.broker.rabbitmq;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitBrokerDefaultsTest {

    @Test
    void consumesInOrderRepublishesFailedEventsAndRoutesPublishedOnes() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("application",
                Map.of("aktimetrix.events.topic", "order-events")));

        new RabbitBrokerDefaults().postProcessEnvironment(environment, new SpringApplication());

        String consumer = "spring.cloud.stream.rabbit.bindings.processor-in-0.consumer.";
        assertThat(environment.getProperty(consumer + "singleActiveConsumer")).isEqualTo("true");
        assertThat(environment.getProperty(consumer + "republishToDlq")).isEqualTo("true");
        assertThat(environment.getProperty(consumer + "deadLetterExchange")).isEqualTo("order-events.dlq");
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.bindings.dead-letter-out-0.producer.routingKeyExpression"))
                .isEqualTo("'order-events.dlq'");
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.bindings.step-instance-out-0.producer.routingKeyExpression"))
                .isEqualTo("headers['aktimetrixKey']");
    }

    @Test
    void partitionsTheEventsQueueWhenAskedTo() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("application",
                Map.of("aktimetrix.events.partitions", "4", "aktimetrix.events.partition", "2")));

        new RabbitBrokerDefaults().postProcessEnvironment(environment, new SpringApplication());

        String consumer = "spring.cloud.stream.bindings.processor-in-0.consumer.";
        assertThat(environment.getProperty(consumer + "partitioned")).isEqualTo("true");
        assertThat(environment.getProperty(consumer + "instanceCount")).isEqualTo("4");
        assertThat(environment.getProperty(consumer + "instanceIndex")).isEqualTo("2");
    }

    @Test
    void oneQueueByDefault() {
        StandardEnvironment environment = new StandardEnvironment();

        new RabbitBrokerDefaults().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.cloud.stream.bindings.processor-in-0.consumer.partitioned")).isNull();
    }

    @Test
    void everyEventOfAnEntityGoesToTheSamePartition() {
        assertThat(RabbitEventPartitions.partition("1234", 4)).isEqualTo(RabbitEventPartitions.partition("1234", 4))
                .isBetween(0, 3);
        // CRC-32 of "1234" is 0x9BE3E0A3 (2615402659): partition 2615402659 % 4 = 3
        assertThat(RabbitEventPartitions.routingKey("order-events", "1234", 4)).isEqualTo("order-events-3");
        assertThat(RabbitEventPartitions.partition("1234", 1)).isZero();
    }

    @Test
    void declaresTheDeadLetterExchangeAndQueue() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("application",
                Map.of("aktimetrix.events.topic", "order-events")));

        Declarables declarables = new RabbitDeadLetterAutoConfiguration().aktimetrixDeadLetters(environment);

        assertThat(declarables.getDeclarablesByType(DirectExchange.class)).singleElement()
                .satisfies(exchange -> assertThat(exchange.getName()).isEqualTo("order-events.dlq"));
        assertThat(declarables.getDeclarablesByType(Queue.class)).singleElement()
                .satisfies(queue -> assertThat(queue.getName()).isEqualTo("order-events.dlq"));
    }
}
