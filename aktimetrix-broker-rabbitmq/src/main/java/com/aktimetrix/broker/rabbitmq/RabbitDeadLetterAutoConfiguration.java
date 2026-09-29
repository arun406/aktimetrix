package com.aktimetrix.broker.rabbitmq;

import com.aktimetrix.autoconfigure.AktimetrixDefaultProperties;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Declares the dead-letter channel on RabbitMQ: a durable direct exchange named {@code aktimetrix.events.dead-letter.topic},
 * and a durable queue of the same name bound to it with that name as routing key. Events that fail, and invalid
 * events, are published there and kept for inspection and replay.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Declarables.class)
@ConditionalOnProperty(prefix = "aktimetrix.events.dead-letter", name = "enabled", matchIfMissing = true)
public class RabbitDeadLetterAutoConfiguration {

    @Bean
    public Declarables aktimetrixDeadLetters(Environment environment) {
        final String name = environment.resolvePlaceholders(AktimetrixDefaultProperties.DEAD_LETTER_TOPIC);
        final DirectExchange exchange = new DirectExchange(name, true, false);
        final Queue queue = new Queue(name, true);
        return new Declarables(exchange, queue, BindingBuilder.bind(queue).to(exchange).with(name));
    }
}
