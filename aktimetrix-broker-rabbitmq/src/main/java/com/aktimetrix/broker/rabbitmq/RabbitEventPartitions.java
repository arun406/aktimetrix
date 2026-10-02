package com.aktimetrix.broker.rabbitmq;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * Where a source system publishes an entity's events when they are partitioned ({@code aktimetrix.events.partitions}
 * above 1): to the events exchange, with the routing key {@code topic-N}, where {@code N} is the entity's partition.
 * <p>
 * The partition is the CRC-32 of the entity id, as UTF-8, modulo the number of partitions: a checksum every platform
 * provides, so a source system written in any language computes the same one. Every event of an entity goes to the
 * same partition, and so is processed in order.
 */
public final class RabbitEventPartitions {

    private RabbitEventPartitions() {
    }

    /**
     * The partition of the entity, from {@code 0} to {@code partitions - 1}.
     */
    public static int partition(String entityId, int partitions) {
        if (partitions < 1) {
            throw new IllegalArgumentException("partitions must be 1 or more, not " + partitions);
        }
        final CRC32 crc = new CRC32();
        crc.update(entityId.getBytes(StandardCharsets.UTF_8));
        return (int) (crc.getValue() % partitions);
    }

    /**
     * The routing key to publish the entity's events with, on the events exchange {@code topic}.
     */
    public static String routingKey(String topic, String entityId, int partitions) {
        return topic + "-" + partition(entityId, partitions);
    }
}
