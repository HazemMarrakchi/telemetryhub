package com.telemetryhub.maintenance.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@ConfigurationPropertiesScan("com.telemetryhub.maintenance.config")
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}