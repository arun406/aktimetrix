package com.aktimetrix.core.store;

import com.aktimetrix.core.model.Alarm;

import java.time.Instant;
import java.util.List;

/**
 * Stores the alarms set at the deadlines of steps and processes.
 */
public interface AlarmStore {

    /**
     * Sets the alarm, replacing the one with the same id, and releasing any claim on it.
     */
    void schedule(Alarm alarm);

    /**
     * Removes the alarm with the id, if any.
     */
    void cancel(String id);

    /**
     * Atomically claims up to {@code limit} alarms due before {@code now} that no one holds, or whose lease has
     * expired, earliest first: sets their {@code lockedUntil} to {@code leaseUntil} and increments their
     * {@code attempts}. Two callers never claim the same alarm while its lease holds.
     */
    List<Alarm> claimDue(Instant now, Instant claimedAt, Instant leaseUntil, int limit);

    long countPending();
}
