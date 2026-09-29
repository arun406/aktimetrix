package com.aktimetrix.it.support;

import org.apache.qpid.server.SystemLauncher;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * An embedded AMQP 0-9-1 broker (Apache Qpid Broker-J), which speaks the protocol of RabbitMQ, and which the Spring
 * Cloud Stream RabbitMQ binder connects to as it would to RabbitMQ.
 * <p>
 * Qpid supports the AMQP features Aktimetrix's RabbitMQ settings rely on, except RabbitMQ's single-active-consumer
 * queue argument, which is turned off here.
 * <p>
 * Outbound exchanges are published to before any consumer exists, so each outbound binding has a required group,
 * {@value #GROUP}: the binder then declares a queue bound to its exchange, {@code <binding>.verifier}, from which the
 * test reads.
 */
public class RabbitTestBroker implements TestBroker {

    public static final String GROUP = "verifier";
    private static final String[] OUTBOUND = {"step-instance-out-0", "process-instance-out-0", "measurement-instance-out-0"};

    private final SystemLauncher broker = new SystemLauncher();
    private final int port;
    private final String eventsTopic;
    private final CachingConnectionFactory connections;
    private final RabbitTemplate rabbit;

    public RabbitTestBroker(String eventsTopic) {
        this.eventsTopic = eventsTopic;
        this.port = freePort();
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("type", "Memory");
        attributes.put("initialConfigurationLocation",
                RabbitTestBroker.class.getResource("/qpid/initial-config.json").toExternalForm());
        attributes.put("startupLoggedToSystemOut", false);
        attributes.put("context", Map.of("qpid.amqp_port", String.valueOf(port),
                "qpid.work_dir", System.getProperty("java.io.tmpdir") + "/qpid-" + port));
        try {
            broker.startup(attributes);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot start the embedded AMQP broker", e);
        }
        connections = new CachingConnectionFactory("localhost", port);
        connections.setUsername("guest");
        connections.setPassword("guest");
        rabbit = new RabbitTemplate(connections);
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Map<String, Object> properties() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("spring.cloud.stream.defaultBinder", "rabbit");
        properties.put("spring.rabbitmq.host", "localhost");
        properties.put("spring.rabbitmq.port", port);
        properties.put("spring.rabbitmq.username", "guest");
        properties.put("spring.rabbitmq.password", "guest");
        // Qpid does not support RabbitMQ's single-active-consumer queue argument; the test has one consumer anyway
        properties.put("spring.cloud.stream.rabbit.bindings.processor-in-0.consumer.singleActiveConsumer", "false");
        for (String binding : OUTBOUND) {
            properties.put("spring.cloud.stream.bindings." + binding + ".producer.requiredGroups", GROUP);
        }
        return properties;
    }

    /**
     * Publishes to the events exchange, a topic exchange declared by the binder when the application starts.
     */
    @Override
    public void send(String destination, String key, String payload) {
        rabbit.send(destination, key, new Message(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public List<String> received(String destination, Predicate<List<String>> enough) {
        final String queue = destination.equals(eventsTopic + ".dlq") ? destination : destination + "." + GROUP;
        final List<String> values = new ArrayList<>();
        final long deadline = System.currentTimeMillis() + 20_000;
        while (!enough.test(values) && System.currentTimeMillis() < deadline) {
            try {
                final Message message = rabbit.receive(queue, 500);
                if (message != null) {
                    values.add(new String(message.getBody(), StandardCharsets.UTF_8));
                }
            } catch (AmqpException e) {
                // the queue is declared when the relay first publishes to the binding: wait for it
                pause();
            }
        }
        return values;
    }

    private static void pause() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        connections.destroy();
        broker.shutdown();
    }
}
