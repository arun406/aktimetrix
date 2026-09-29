package com.aktimetrix.store.mongodb;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.store.AktimetrixTransactions;
import com.aktimetrix.core.store.DefinitionStore;
import com.aktimetrix.core.store.MeasurementInstanceStore;
import com.aktimetrix.core.store.OutboxStore;
import com.aktimetrix.core.store.ProcessInstanceStore;
import com.aktimetrix.core.store.StepInstanceStore;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

/**
 * Keeps Aktimetrix's state in MongoDB, through the application's {@link MongoTemplate}. Used when
 * {@code aktimetrix.storage.type=mongodb}, or when it is the only store module on the classpath.
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(MongoDataAutoConfiguration.class)
@ConditionalOnProperty(prefix = "aktimetrix.storage", name = "type", havingValue = "mongodb", matchIfMissing = true)
@ConditionalOnMissingBean(ProcessInstanceStore.class)
public class MongoStoreAutoConfiguration {

    /**
     * Stores {@link ZonedDateTime}s, which MongoDB has no type for, as dates in UTC.
     */
    @Bean
    @ConditionalOnMissingBean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new ZonedDateTimeReadConverter(), new ZonedDateTimeWriteConverter()));
    }

    @Bean
    public ProcessInstanceStore aktimetrixProcessInstanceStore(MongoTemplate mongoTemplate, MongoStoreInitializer database) {
        return new MongoInstanceStores.Processes(mongoTemplate);
    }

    @Bean
    public StepInstanceStore aktimetrixStepInstanceStore(MongoTemplate mongoTemplate, MongoStoreInitializer database) {
        return new MongoInstanceStores.Steps(mongoTemplate);
    }

    @Bean
    public MeasurementInstanceStore aktimetrixMeasurementInstanceStore(MongoTemplate mongoTemplate, MongoStoreInitializer database) {
        return new MongoInstanceStores.Measurements(mongoTemplate);
    }

    @Bean
    public DefinitionStore aktimetrixDefinitionStore(MongoTemplate mongoTemplate, MongoStoreInitializer database) {
        return new MongoDefinitionStore(mongoTemplate);
    }

    @Bean
    public OutboxStore aktimetrixOutboxStore(MongoTemplate mongoTemplate, MongoStoreInitializer database) {
        return new MongoOutboxStore(mongoTemplate);
    }

    @Bean
    public AktimetrixTransactions aktimetrixTransactions(MongoDatabaseFactory databaseFactory,
                                                         MongoTemplate mongoTemplate, AktimetrixProperties properties) {
        return new MongoTransactions(databaseFactory, mongoTemplate, properties);
    }

    /**
     * The stores depend on it, so the database is upgraded and indexed before they are first used.
     */
    @Bean
    public MongoStoreInitializer aktimetrixMongoStoreInitializer(MongoTemplate mongoTemplate,
                                                                 AktimetrixProperties properties) {
        return new MongoStoreInitializer(mongoTemplate, properties);
    }

    @ReadingConverter
    static class ZonedDateTimeReadConverter implements Converter<Date, ZonedDateTime> {
        @Override
        public ZonedDateTime convert(Date date) {
            return date.toInstant().atZone(ZoneOffset.UTC);
        }
    }

    @WritingConverter
    static class ZonedDateTimeWriteConverter implements Converter<ZonedDateTime, Date> {
        @Override
        public Date convert(ZonedDateTime zonedDateTime) {
            return Date.from(zonedDateTime.toInstant());
        }
    }
}
