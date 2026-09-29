package com.aktimetrix.store.memory;

import com.aktimetrix.core.store.AktimetrixTransactions;

/**
 * Runs units of work one at a time, without rollback: a unit of work that fails part-way keeps what it saved before
 * failing. Acceptable for tests and demos; use a transactional store in production.
 */
final class MemoryTransactions implements AktimetrixTransactions {

    @Override
    public synchronized void run(Runnable work) {
        work.run();
    }

    @Override
    public boolean isAtomic() {
        return false;
    }
}
