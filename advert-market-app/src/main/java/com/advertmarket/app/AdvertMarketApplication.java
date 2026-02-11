package com.advertmarket.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.advertmarket")
public class AdvertMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdvertMarketApplication.class, args);
    }
}