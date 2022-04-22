package com.aktimetrix.service.processor;

import com.aktimetrix.core.exception.ProcessorException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@ComponentScan(basePackages = {"com.aktimetrix.service.processor", "com.aktimetrix.core"})
@SpringBootApplication
@RequiredArgsConstructor
public class ProcessManagerApplication {
    final private static Logger logger = LoggerFactory.getLogger(ProcessManagerApplication.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        SpringApplication.run(ProcessManagerApplication.class, args);
    }

    @Bean
    public Consumer<Message<String>> processor() throws ProcessorException {
        return payload -> {
            logger.debug(payload.getPayload());
        };
    }
}
