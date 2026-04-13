package com.carrefourbank.dailybalance.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.Date;

@Configuration
public class BannerConfig {

    @Value("${spring.application.name:dailybalance-service}")
    private String applicationName;

    @Bean
    public ApplicationListener<ApplicationStartedEvent> startupDateInitializer() {
        return event -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = dateFormat.format(new Date());
            System.setProperty("application.formatted-date", formattedDate);
        };
    }
}