package com.advertmarket.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/** Main application entry point. */
@EnableKafka
@SpringBootApplication(scanBasePackages = "com.advertmarket")
public class AdvertMarketApplication {

    /** Application entry point. */
    public static void main(String[] args) {
        SpringApplication.run(AdvertMarketApplication.class, args);
    }
}
