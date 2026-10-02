package com.aktimetrix.it;

import com.aktimetrix.broker.rabbitmq.RabbitEventPartitions;
import com.aktimetrix.it.support.RabbitTestBroker;
import com.aktimetrix.it.support.TestMonitor;
import com.aktimetrix.it.support.TestStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.stream.IntStream;

import static com.aktimetrix.it.support.TestMonitor.await;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two instances on RabbitMQ, each consuming one of two partitions of the events: an entity's events reach the
 * instance of its partition only, so instances share the load and each entity's events stay in order.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RabbitPartitionsTest {

    private static final String TOPIC = "partitioned-events";

    private RabbitTestBroker broker;
    private TestStore store;
    private TestMonitor first;
    private TestMonitor second;

    @BeforeAll
    void start() {
        broker = new RabbitTestBroker(TOPIC);
        store = TestStore.memory();
        first = monitor(0);
        second = monitor(1);
    }

    private TestMonitor monitor(int partition) {
        return new TestMonitor(ParcelMonitor.class, "T1", broker, store, "aktimetrix.events.topic=" + TOPIC,
                "aktimetrix.events.partitions=2", "aktimetrix.events.partition=" + partition);
    }

    @AfterAll
    void stop() {
        first.close();
        second.close();
        store.close();
        broker.close();
    }

    @Test
    void eachInstanceProcessesTheEntitiesOfItsPartition() {
        final String inFirst = parcelIn(0);
        final String inSecond = parcelIn(1);

        book(inFirst);
        book(inSecond);

        await(() -> first.process("PARCEL", inFirst), p -> p.getId() != null, "the first instance's parcel");
        await(() -> second.process("PARCEL", inSecond), p -> p.getId() != null, "the second instance's parcel");
        assertThat(first.process("PARCEL", inSecond)).isEmpty();
        assertThat(second.process("PARCEL", inFirst)).isEmpty();
    }

    private static String parcelIn(int partition) {
        return IntStream.range(0, 100).mapToObj(i -> "P-" + i)
                .filter(id -> RabbitEventPartitions.partition(id, 2) == partition).findFirst().orElseThrow();
    }

    private void book(String parcel) {
        broker.send(TOPIC, RabbitEventPartitions.routingKey(TOPIC, parcel, 2), "{\"tenantKey\":\"T1\",\"eventId\":\"book-"
                + parcel + "\",\"eventCode\":\"PARCEL_BOOKED\",\"entityType\":\"parcel\",\"entityId\":\"" + parcel
                + "\",\"eventUTCTime\":\"2024-01-10 09:00:00\",\"entity\":{\"bookedAt\":\"2024-01-10 09:00:00\"}}");
    }
}
