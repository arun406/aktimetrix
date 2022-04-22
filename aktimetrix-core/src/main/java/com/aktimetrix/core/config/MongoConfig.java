package com.aktimetrix.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"com.aktimetrix.core.repository",
        "com.aktimetrix.core.referencedata.repository",
        "com.aktimetrix.core.tenant.repository",
        "com.aktimetrix.service.processor.ciq.cdmpc.repository",
        "com.aktimetrix.service.processor.ciq.cdmpc.referencedata.encore.repository"})
public class MongoConfig {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

}
