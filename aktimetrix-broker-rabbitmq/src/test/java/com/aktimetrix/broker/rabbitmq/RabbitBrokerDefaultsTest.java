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
