package com.advertmarket.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Main application entry point. */
@SpringBootApplication(scanBasePackages = "com.advertmarket")
public class AdvertMarketApplication {

    /** Application entry point. */
    public static void main(String[] args) {
        SpringApplication.run(AdvertMarketApplication.class, args);
    }
}
