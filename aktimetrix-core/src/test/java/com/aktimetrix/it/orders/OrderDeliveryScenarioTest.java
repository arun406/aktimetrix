package com.aktimetrix.it.orders;

import com.aktimetrix.core.api.Conformance;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.it.ParcelMonitor;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The order delivery use case of the white paper (section 1.1), end to end.
 */
@SpringBootTest(classes = {ParcelMonitor.class, OrderDeliveryScenarioTest.Metrics.class}, properties = {
        "aktimetrix.events.topic=shop-events", "aktimetrix.monitor.enabled=false"})
@EmbeddedKafka(partitions = 1, topics = {"shop-events", "step-instance-out-0", "process-instance-out-0",
        "measurement-instance-out-0", "shop-events.dlq"})
class OrderDeliveryScenarioTest {

    private static final MongoServer MONGO = new MongoServer(new MemoryBackend());
    private static final InetSocketAddress MONGO_ADDRESS = MONGO.bind();

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:" + MONGO_ADDRESS.getPort() + "/shop");
        registry.add("spring.cloud.stream.kafka.binder.brokers", () -> "${spring.embedded.kafka.brokers}");
    }

    @AfterAll
    static void stopMongo() {
        MONGO.shutdown();
    }

    @TestConfiguration
    static class Metrics {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Autowired
    private EmbeddedKafkaBroker kafka;
    @Autowired
    private MongoTemplate mongo;

    /**
     * A priority customer's order, created at 09:00. Rules plan it: the order within 1 day, the delivery step within
     * 4 hours. Time is compared on every step; distance and fuel while travelling, the parcel's temperature on
     * delivery, and the customer's rating the next morning, after the order completed, are compared with their plans.
     */
    @Test
    void comparesPlanAndActualInEveryDimension() throws Exception {
        send("ORDER_CREATED", "2024-03-01 09:00:00", "{\"priority\":true,\"createdAt\":\"2024-03-01 09:00:00\"}");
        awaitTrue(() -> mongo.findAll(StepInstance.class).size() == 7);
        send("ORDER_CONFIRMED", "2024-03-01 09:02:00", null);
        send("PAYMENT_CONFIRMED", "2024-03-01 09:20:00", null);
        send("HANDED_TO_AGENT", "2024-03-01 11:40:00", null);
        send("AGENT_ACCEPTED", "2024-03-01 11:50:00", null);
        send("TRAVEL_STARTED", "2024-03-01 11:55:00", null);
        send("ARRIVED", "2024-03-01 12:45:00", "{\"route\":{\"distanceKm\":12},\"fuelLitres\":1.0}");
        send("DELIVERED", "2024-03-01 12:55:00", "{\"parcelTemperatureC\":40}");

        awaitTrue(() -> "Completed".equals(process().getStatus()));
        send("RATED", "2024-03-02 08:00:00", "{\"review\":{\"stars\":4}}");
        awaitTrue(() -> "Completed".equals(step("RATED").getStatus()));
        ProcessInstance order = process();
        assertThat(order.getPlannedAt()).isEqualTo(LocalDateTime.of(2024, 3, 2, 9, 0));
        assertThat(order.getTimeliness()).isEqualTo(Timeliness.ON_TIME);

        // the planning rule: priority customers are delivered within 4 hours
        assertThat(step("DELIVERED").getPlannedAt()).isEqualTo(LocalDateTime.of(2024, 3, 1, 13, 0));
        assertThat(step("CONFIRM").getTimeliness()).isEqualTo(Timeliness.ON_TIME);
        assertThat(step("PAY").getTimeliness()).isEqualTo(Timeliness.ON_TIME);
        assertThat(step("HANDOVER").getTimeliness()).isEqualTo(Timeliness.LATE);
        assertThat(step("TRAVEL").getTimeliness()).isEqualTo(Timeliness.LATE);
        assertThat(step("DELIVERED").getTimeliness()).isEqualTo(Timeliness.ON_TIME);

        assertActual("TRAVEL", "TIME", "PT25M", Conformance.OUT_OF_TOLERANCE);
        assertActual("TRAVEL", "DISTANCE", "7", Conformance.OUT_OF_TOLERANCE);
        assertActual("TRAVEL", "FUEL", "0.6", Conformance.OUT_OF_TOLERANCE);
        assertActual("DELIVERED", "TEMPERATURE", "10", Conformance.OUT_OF_TOLERANCE);
        assertActual("DELIVERED", "TIME", "PT-5M", Conformance.WITHIN_TOLERANCE);
        // rated the next morning, after the order completed: one star below plan, within tolerance
        assertActual("RATED", "RATING", "-1", Conformance.WITHIN_TOLERANCE);
    }

    private ProcessInstance process() {
        return mongo.findAll(ProcessInstance.class).get(0);
    }

    private StepInstance step(String code) {
        return mongo.findAll(StepInstance.class).stream().filter(s -> code.equals(s.getStepCode())).findFirst().orElseThrow();
    }

    private void assertActual(String step, String code, String deviation, Conformance conformance) {
        MeasurementInstance actual = mongo.findAll(MeasurementInstance.class).stream()
                .filter(m -> step.equals(m.getStepCode()) && code.equals(m.getCode()) && "A".equals(m.getType()))
                .findFirst().orElseThrow(() -> new AssertionError("no actual " + code + " for " + step));
        assertThat(actual.getDeviation()).as(step + " " + code).isEqualTo(deviation);
        assertThat(actual.getConformance()).as(step + " " + code).isEqualTo(conformance);
    }

    private void send(String code, String utc, String entity) {
        String event = "{\"tenantKey\":\"SHOP\",\"eventCode\":\"" + code + "\",\"entityType\":\"order\","
                + "\"entityId\":\"1234\",\"eventUTCTime\":\"" + utc + "\"" + (entity == null ? "" : ",\"entity\":" + entity) + "}";
        KafkaTemplate<String, String> template = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(kafka), new StringSerializer(), new StringSerializer()));
        template.send("shop-events", "1234", event);
        template.flush();
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 20_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) throw new AssertionError("timeout");
            Thread.sleep(100);
        }
    }
}
