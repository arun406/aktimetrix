package com.aktimetrix.core.service;

import com.aktimetrix.core.transferobjects.EventContext.Cause;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * What the current unit of work is processing: set by the inbound consumer for a business event, and by the overdue
 * monitors for a deadline check, so that every event published during it records its cause and business time.
 * <p>
 * Internal: not part of the public API.
 */
public final class ProcessingContext {

    private static final ThreadLocal<Current> CURRENT = new ThreadLocal<>();

    private ProcessingContext() {
    }

    /**
     * Runs the work with the given cause and business time as the current ones.
     */
    public static void run(Cause cause, LocalDateTime occurredAt, Runnable work) {
        final Current previous = CURRENT.get();
        CURRENT.set(new Current(cause, occurredAt));
        try {
            work.run();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static Cause cause() {
        final Current current = CURRENT.get();
        return current == null ? null : current.cause;
    }

    /**
     * The business time of the current unit of work, or {@code fallback} outside of one.
     */
    public static LocalDateTime occurredAt(Supplier<LocalDateTime> fallback) {
        final Current current = CURRENT.get();
        return current == null || current.occurredAt == null ? fallback.get() : current.occurredAt;
    }

    private static final class Current {
        private final Cause cause;
        private final LocalDateTime occurredAt;

        private Current(Cause cause, LocalDateTime occurredAt) {
            this.cause = cause;
            this.occurredAt = occurredAt;
        }
    }
}
