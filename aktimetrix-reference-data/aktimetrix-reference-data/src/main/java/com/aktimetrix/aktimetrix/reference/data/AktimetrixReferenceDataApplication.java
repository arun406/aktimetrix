package com.aktimetrix.aktimetrix.reference.data;

import com.aktimetrix.aktimetrix.reference.data.config.FileStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({FileStorageProperties.class})
public class AktimetrixReferenceDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(AktimetrixReferenceDataApplication.class, args);
    }

}
