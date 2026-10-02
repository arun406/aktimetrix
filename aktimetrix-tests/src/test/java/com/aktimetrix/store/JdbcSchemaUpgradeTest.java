package com.aktimetrix.store;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.store.jdbc.JdbcStoreInitializer;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A database created by an earlier version, with one process instance per entity, is upgraded in place to one per
 * entity and run, keeping its instances as run 1.
 */
class JdbcSchemaUpgradeTest {

    @Test
    void anEarlierTableGainsRunsAndKeepsItsInstances() {
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:upgrade;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE aktimetrix_process_instance (id VARCHAR(64) PRIMARY KEY, tenant VARCHAR(255) NOT NULL, "
                + "process_code VARCHAR(255) NOT NULL, entity_type VARCHAR(255), entity_id VARCHAR(255) NOT NULL, "
                + "status VARCHAR(32), complete BOOLEAN NOT NULL, late_after TIMESTAMP, timeliness VARCHAR(16), "
                + "revision BIGINT NOT NULL, document TEXT NOT NULL, "
                + "CONSTRAINT aktimetrix_process_entity UNIQUE (tenant, process_code, entity_type, entity_id))");
        jdbc.update("INSERT INTO aktimetrix_process_instance VALUES ('p1', 'AA', 'ORDER_DELIVERY', 'order', '1234', "
                + "'Completed', TRUE, NULL, NULL, 3, '{}')");

        new JdbcStoreInitializer(dataSource, new AktimetrixProperties()).createSchema();

        assertThat(jdbc.queryForObject("SELECT run_number FROM aktimetrix_process_instance WHERE id = 'p1'",
                Integer.class)).isEqualTo(1);
        jdbc.update("INSERT INTO aktimetrix_process_instance (id, tenant, process_code, entity_type, entity_id, status, "
                + "complete, revision, run_number, document) VALUES ('p2', 'AA', 'ORDER_DELIVERY', 'order', '1234', "
                + "'Created', FALSE, 0, 2, '{}')");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM aktimetrix_process_instance", Integer.class)).isEqualTo(2);
    }
}
