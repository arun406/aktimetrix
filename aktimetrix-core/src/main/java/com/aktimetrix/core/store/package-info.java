/**
 * The state-store contract: what Aktimetrix needs from the database that holds its definitions, instances and
 * outbox. The model and the services use only these interfaces; a store module ({@code aktimetrix-store-mongodb},
 * {@code aktimetrix-store-jdbc} or {@code aktimetrix-store-memory}) implements them.
 * <p>
 * Public API for store implementers. Every implementation must pass the store contract tests in
 * {@code aktimetrix-tests}.
 */
package com.aktimetrix.core.store;
