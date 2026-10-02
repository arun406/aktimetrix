package com.aktimetrix.it;

import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.it.support.RabbitTestBroker;
import com.aktimetrix.it.support.TestMonitor;
import com.aktimetrix.it.support.TestStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.aktimetrix.it.support.TestMonitor.awaitTrue;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two instances on RabbitMQ, with the events split into two partitions. Source systems publish as they always do; the
 * event router reads each event's entity and passes it to that entity's partition, so each entity is processed by
 * one instance, in order, and the instances share the load.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RabbitPartitionsTest {

    private static final String TOPIC = "partitioned-events";

    private RabbitTestBroker broker;
    private TestStore firstStore;
    private TestStore secondStore;
    private TestMonitor first;
    private TestMonitor second;

    @BeforeAll
    void start() {
        broker = new RabbitTestBroker(TOPIC);
        // a store each, to see which instance processed which entity
        firstStore = TestStore.memory();
        secondStore = TestStore.memory();
        first = monitor(firstStore, 0);
        second = monitor(secondStore, 1);
    }

    private TestMonitor monitor(TestStore store, int partition) {
        return new TestMonitor(ParcelMonitor.class, "T1", broker, store, "aktimetrix.events.topic=" + TOPIC,
                "aktimetrix.events.partitions=2", "aktimetrix.events.partition=" + partition);
    }

    @AfterAll
    void stop() {
        first.close();
        second.close();
        firstStore.close();
        secondStore.close();
        broker.close();
    }

    @Test
    void eachEntityIsProcessedByTheInstanceOfItsPartitionAndInOrder() {
        final List<String> parcels = IntStream.range(0, 10).mapToObj(i -> "P-" + i).collect(Collectors.toList());
        // published as a source system always has: to the events exchange, keyed as it likes
        parcels.forEach(parcel -> send(parcel, "book-" + parcel, "PARCEL_BOOKED", "09:00:00",
                ",\"entity\":{\"bookedAt\":\"2024-01-10 09:00:00\"}"));
        parcels.forEach(parcel -> send(parcel, "cancel-" + parcel, "PARCEL_CANCELLED", "10:00:00", ""));

        awaitTrue(() -> parcels.stream().allMatch(parcel -> cancelled(first, parcel) || cancelled(second, parcel)),
                "every parcel booked, then cancelled");
        for (String parcel : parcels) {
            assertThat(first.process("PARCEL", parcel).isPresent())
                    .as("%s is processed by one instance only", parcel)
                    .isNotEqualTo(second.process("PARCEL", parcel).isPresent());
        }
        assertThat(parcels.stream().filter(parcel -> first.process("PARCEL", parcel).isPresent()))
                .as("both instances share the load").isNotEmpty().hasSizeLessThan(parcels.size());
    }

    private static boolean cancelled(TestMonitor monitor, String parcel) {
        return monitor.process("PARCEL", parcel).map(ProcessInstance::getStatus).filter("Cancelled"::equals).isPresent();
    }

    private void send(String parcel, String eventId, String eventCode, String time, String entity) {
        broker.send(TOPIC, "parcel." + eventCode.toLowerCase(), "{\"tenantKey\":\"T1\",\"eventId\":\"" + eventId
                + "\",\"eventCode\":\"" + eventCode + "\",\"entityType\":\"parcel\",\"entityId\":\"" + parcel
                + "\",\"eventUTCTime\":\"2024-01-10 " + time + "\"" + entity + "}");
    }
}
