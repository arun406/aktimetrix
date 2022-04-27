package com.aktimetrix.service.processor;

import org.javatuples.Pair;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Arrays;

@Configuration
@EnableMongoRepositories(basePackages = {"com.aktimetrix.service.processor.core.repository",
        "com.aktimetrix.service.processor.core.referencedata.repository",
        "com.aktimetrix.service.processor.core.referencedata.tenant.repository",
        "com.aktimetrix.service.processor.ciq.cdmpc.repository",
        "com.aktimetrix.service.processor.ciq.cdmpc.referencedata.encore.repository"})
public class MongoConfig {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }


    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(
                Arrays.asList(
                        new StringToPairConverter(),
                        new PairToStringConverter()));
    }

    @WritingConverter
    public class PairToStringConverter implements Converter<Pair, String> {
        @Override
        public String convert(Pair source) {
            return source.getValue0() + "#" + source.getValue1();
        }
    }

    @ReadingConverter
    public class StringToPairConverter implements Converter<String, Pair> {
        @Override
        public Pair convert(String source) {
            final String[] split = source.split("#");
            return Pair.with(split[0], split[1]);
        }
    }
}
