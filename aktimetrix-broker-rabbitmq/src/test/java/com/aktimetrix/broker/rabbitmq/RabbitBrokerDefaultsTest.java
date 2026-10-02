package com.aktimetrix.broker.rabbitmq;

import com.aktimetrix.autoconfigure.AktimetrixDefaultProperties;
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
    void routesTheEventsToPartitionsWhenAskedTo() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("application",
                Map.of("aktimetrix.events.topic", "order-events", "aktimetrix.events.partitions", "4",
                        "aktimetrix.events.partition", "2")));
        new AktimetrixDefaultProperties().postProcessEnvironment(environment, new SpringApplication());

        new RabbitBrokerDefaults().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.cloud.function.definition")).isEqualTo("eventRouter;processor");
        String bindings = "spring.cloud.stream.bindings.";
        // the router consumes the queue the sources already publish to, in order and in a transaction
        assertThat(environment.getProperty(bindings + "eventRouter-in-0.destination")).isEqualTo("order-events");
        assertThat(environment.getProperty(bindings + "eventRouter-in-0.group")).isEqualTo("aktimetrix");
        String router = "spring.cloud.stream.rabbit.bindings.eventRouter-";
        assertThat(environment.getProperty(router + "in-0.consumer.singleActiveConsumer")).isEqualTo("true");
        assertThat(environment.getProperty(router + "in-0.consumer.transacted")).isEqualTo("true");
        assertThat(environment.getProperty(router + "out-0.producer.transacted")).isEqualTo("true");
        // and republishes by entity to the partitions, all declared up front
        assertThat(environment.getProperty(bindings + "eventRouter-out-0.destination"))
                .isEqualTo("order-events.partitioned");
        assertThat(environment.getProperty(bindings + "eventRouter-out-0.producer.partitionKeyExpression"))
                .isEqualTo("headers['aktimetrixPartitionKey']");
        assertThat(environment.getProperty(bindings + "eventRouter-out-0.producer.partitionCount")).isEqualTo("4");
        assertThat(environment.getProperty(bindings + "eventRouter-out-0.producer.requiredGroups"))
                .isEqualTo("aktimetrix");
        // each instance processes its partition, overriding the broker-neutral default destination
        assertThat(environment.getProperty(bindings + "processor-in-0.destination")).isEqualTo("order-events.partitioned");
        assertThat(environment.getProperty(bindings + "processor-in-0.consumer.partitioned")).isEqualTo("true");
        assertThat(environment.getProperty(bindings + "processor-in-0.consumer.instanceCount")).isEqualTo("4");
        assertThat(environment.getProperty(bindings + "processor-in-0.consumer.instanceIndex")).isEqualTo("2");
    }

    @Test
    void oneQueueByDefault() {
        StandardEnvironment environment = new StandardEnvironment();
        new AktimetrixDefaultProperties().postProcessEnvironment(environment, new SpringApplication());

        new RabbitBrokerDefaults().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.cloud.function.definition")).isEqualTo("processor");
        assertThat(environment.getProperty("spring.cloud.stream.bindings.processor-in-0.consumer.partitioned")).isNull();
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
