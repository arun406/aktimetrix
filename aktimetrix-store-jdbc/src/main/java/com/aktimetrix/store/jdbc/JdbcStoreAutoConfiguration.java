package com.aktimetrix.store.jdbc;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.store.AktimetrixTransactions;
import com.aktimetrix.core.store.DefinitionStore;
import com.aktimetrix.core.store.MeasurementInstanceStore;
import com.aktimetrix.core.store.OutboxStore;
import com.aktimetrix.core.store.ProcessInstanceStore;
import com.aktimetrix.core.store.StepInstanceStore;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Keeps Aktimetrix's state in the application's relational database, through its {@link DataSource}. Used when
 * {@code aktimetrix.storage.type=jdbc}, or when it is the only store module on the classpath.
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(value = {DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class},
        name = "com.aktimetrix.store.mongodb.MongoStoreAutoConfiguration")
@ConditionalOnProperty(prefix = "aktimetrix.storage", name = "type", havingValue = "jdbc", matchIfMissing = true)
@ConditionalOnMissingBean(ProcessInstanceStore.class)
public class JdbcStoreAutoConfiguration {

    @Bean
    public ProcessInstanceStore aktimetrixProcessInstanceStore(DataSource dataSource) {
        return new JdbcInstanceStores.Processes(new JdbcTemplate(dataSource));
    }

    @Bean
    public StepInstanceStore aktimetrixStepInstanceStore(DataSource dataSource) {
        return new JdbcInstanceStores.Steps(new JdbcTemplate(dataSource));
    }

    @Bean
    public MeasurementInstanceStore aktimetrixMeasurementInstanceStore(DataSource dataSource) {
        return new JdbcInstanceStores.Measurements(new JdbcTemplate(dataSource));
    }

    @Bean
    public DefinitionStore aktimetrixDefinitionStore(DataSource dataSource) {
        return new JdbcDefinitionStore(new JdbcTemplate(dataSource));
    }

    @Bean
    public OutboxStore aktimetrixOutboxStore(DataSource dataSource) {
        return new JdbcOutboxStore(new JdbcTemplate(dataSource));
    }

    @Bean
    public AktimetrixTransactions aktimetrixTransactions(DataSource dataSource, AktimetrixProperties properties) {
        return new JdbcTransactions(dataSource, properties);
    }

    @Bean
    public JdbcStoreInitializer aktimetrixJdbcStoreInitializer(DataSource dataSource, AktimetrixProperties properties) {
        return new JdbcStoreInitializer(dataSource, properties);
    }
}
