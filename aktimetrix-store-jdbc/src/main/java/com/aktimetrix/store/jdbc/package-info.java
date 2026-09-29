/**
 * The JDBC implementation of the Aktimetrix state-store contract, written for PostgreSQL. Each object is kept as a JSON
 * document, next to the columns its queries, constraints and version checks need; the tables are created at startup.
 */
package com.aktimetrix.store.jdbc;
