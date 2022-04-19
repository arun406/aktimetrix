package com.aktimetrix.service.planner;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;

@Configuration
@EnableMongoRepositories(basePackages = {"com.aktimetrix.service.planner.repository"})
public class MongoConfig {
    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(
                Arrays.asList(
                        new ZonedDateTimeReadConverter(),
                        new ZonedDateTimeWriteConverter()));
    }

    @ReadingConverter
    public static class ZonedDateTimeReadConverter implements Converter<Date, ZonedDateTime> {
        @Override
        public ZonedDateTime convert(Date date) {
            return date.toInstant().atZone(ZoneOffset.UTC);
        }
    }

    @WritingConverter
    public static class ZonedDateTimeWriteConverter implements Converter<ZonedDateTime, Date> {
        @Override
        public Date convert(ZonedDateTime zonedDateTime) {
            return Date.from(zonedDateTime.toInstant());
        }
    }
}
