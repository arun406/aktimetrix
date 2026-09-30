package com.aktimetrix.store.memory;

import com.aktimetrix.core.store.AktimetrixTransactions;
import com.aktimetrix.core.store.AlarmStore;
import com.aktimetrix.core.store.DefinitionStore;
import com.aktimetrix.core.store.MeasurementInstanceStore;
import com.aktimetrix.core.store.OutboxStore;
import com.aktimetrix.core.store.ProcessInstanceStore;
import com.aktimetrix.core.store.StepInstanceStore;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keeps Aktimetrix's state in memory. Used when {@code aktimetrix.storage.type=memory}, or when it is the only store
 * module on the classpath.
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(name = {"com.aktimetrix.store.mongodb.MongoStoreAutoConfiguration",
        "com.aktimetrix.store.jdbc.JdbcStoreAutoConfiguration"})
@ConditionalOnProperty(prefix = "aktimetrix.storage", name = "type", havingValue = "memory", matchIfMissing = true)
@ConditionalOnMissingBean(ProcessInstanceStore.class)
public class MemoryStoreAutoConfiguration {

    public MemoryStoreAutoConfiguration() {
        LoggerFactory.getLogger(MemoryStoreAutoConfiguration.class).warn("Aktimetrix keeps its state in memory: it is "
                + "lost when the application stops and not shared between instances. Use it for tests and demos only.");
    }

    @Bean
    public ProcessInstanceStore aktimetrixProcessInstanceStore() {
        return new MemoryInstanceStores.Processes();
    }

    @Bean
    public StepInstanceStore aktimetrixStepInstanceStore() {
        return new MemoryInstanceStores.Steps();
    }

    @Bean
    public MeasurementInstanceStore aktimetrixMeasurementInstanceStore() {
        return new MemoryInstanceStores.Measurements();
    }

    @Bean
    public DefinitionStore aktimetrixDefinitionStore() {
        return new MemoryDefinitionStore();
    }

    @Bean
    public OutboxStore aktimetrixOutboxStore() {
        return new MemoryOutboxStore();
    }

    @Bean
    public AlarmStore aktimetrixAlarmStore() {
        return new MemoryAlarmStore();
    }

    @Bean
    public AktimetrixTransactions aktimetrixTransactions() {
        return new MemoryTransactions();
    }
}
