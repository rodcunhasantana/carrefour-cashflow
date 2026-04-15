package com.carrefourbank.transaction.infrastructure.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class BannerConfig {
    // application.formatted-date é definido em TransactionServiceApplication.main()
    // antes do SpringApplication.run(), garantindo que o banner.txt consiga resolver a property.
}