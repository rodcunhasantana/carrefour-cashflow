package com.carrefourbank.dailybalance.infrastructure.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class BannerConfig {
    // application.formatted-date é definido em DailybalanceServiceApplication.main()
    // antes do SpringApplication.run(), garantindo que o banner.txt consiga resolver a property.
}