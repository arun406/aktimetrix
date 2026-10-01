package com.aktimetrix.store;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.store.jdbc.JdbcStoreAutoConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * The JDBC store on H2 in PostgreSQL compatibility mode.
 */
class JdbcStoreContractTest extends StoreContractTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AktimetrixProperties.class)
    @ImportAutoConfiguration({DataSourceAutoConfiguration.class, JdbcStoreAutoConfiguration.class})
    static class Store {
    }

    @Override
    protected ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(Store.class).web(WebApplicationType.NONE)
                .properties("aktimetrix.storage.type=jdbc",
                        "spring.datasource.url=jdbc:h2:mem:contract;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
                .run();
    }
}
