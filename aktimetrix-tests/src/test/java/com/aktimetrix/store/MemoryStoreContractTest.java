package com.aktimetrix.store;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.store.memory.MemoryStoreAutoConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

class MemoryStoreContractTest extends StoreContractTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AktimetrixProperties.class)
    @ImportAutoConfiguration(MemoryStoreAutoConfiguration.class)
    static class Store {
    }

    @Override
    protected ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(Store.class).web(WebApplicationType.NONE)
                .properties("aktimetrix.storage.type=memory").run();
    }
}
