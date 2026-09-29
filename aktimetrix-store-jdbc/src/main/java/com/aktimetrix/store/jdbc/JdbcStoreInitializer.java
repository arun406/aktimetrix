package com.aktimetrix.store.jdbc;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * Creates the Aktimetrix tables and indexes that do not exist yet, before the stores are used. Disable with
 * {@code aktimetrix.storage.create-indexes=false} to manage the schema yourself, from
 * {@code com/aktimetrix/store/jdbc/schema.sql}.
 */
public class JdbcStoreInitializer implements InitializingBean {

    public static final String SCHEMA = "com/aktimetrix/store/jdbc/schema.sql";

    private final DataSource dataSource;
    private final AktimetrixProperties properties;

    public JdbcStoreInitializer(DataSource dataSource, AktimetrixProperties properties) {
        this.dataSource = dataSource;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (properties.getStorage().isCreateIndexes()) {
            createSchema();
        }
    }

    public void createSchema() {
        new ResourceDatabasePopulator(new ClassPathResource(SCHEMA)).execute(dataSource);
        LoggerFactory.getLogger(JdbcStoreInitializer.class).info("Aktimetrix tables are ready");
    }
}
