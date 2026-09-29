package com.aktimetrix.core.store;

import com.aktimetrix.core.outbox.OutboxMessage;

import java.time.Instant;
import java.util.Optional;

/**
 * Stores the messages waiting to be published. Written in the same unit of work as the state they describe.
 */
public interface OutboxStore {

    /**
     * Inserts the message, assigning it an id.
     */
    void add(OutboxMessage message);

    /**
     * Atomically claims the oldest unsent message that no one holds, or whose lease has expired: sets its
     * {@code lockedUntil} to {@code leaseUntil} and increments its {@code attempts}. Two callers never claim the same
     * message while its lease holds.
     *
     * @return the claimed message, or empty when there is none
     */
    Optional<OutboxMessage> claimNext(Instant now, Instant leaseUntil);

    /**
     * Marks the message sent and releases its lease.
     */
    void markSent(String id, Instant sentAt);

    long countPending();

    /**
     * Deletes messages sent before the instant.
     *
     * @return how many were deleted
     */
    long deleteSentBefore(Instant sentBefore);
}
