package com.bookworm.ebookstore.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the store properties and the UTC clock every timestamp comes from, so tests can fix time. */
@Configuration
@EnableConfigurationProperties(StoreProperties.class)
public class StoreConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
