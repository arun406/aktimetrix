package com.aktimetrix.core.store;

/**
 * Runs a unit of work (one business event, or marking one step or process overdue) so that the state it changes and
 * the outbox entries it queues are written together, or not at all.
 */
public interface AktimetrixTransactions {

    /**
     * Runs the work, in a transaction when {@link #isAtomic()}.
     */
    void run(Runnable work);

    /**
     * Whether units of work are atomic. A store that cannot make them atomic still runs them, but a crash in the
     * middle of one can keep its state without its outbound events.
     */
    boolean isAtomic();
}
