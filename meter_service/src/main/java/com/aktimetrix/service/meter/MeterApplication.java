package com.aktimetrix.service.meter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {"com.aktimetrix.service.meter", "com.aktimetrix.core"})
@SpringBootApplication
public class MeterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeterApplication.class, args);
    }
}
