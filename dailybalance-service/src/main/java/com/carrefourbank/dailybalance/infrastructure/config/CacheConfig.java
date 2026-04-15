package com.carrefourbank.dailybalance.infrastructure.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // CaffeineCacheManager is autoconfigured by Spring Boot from application.yaml
    // (spring.cache.type=caffeine, spring.cache.caffeine.spec, spring.cache.cache-names)
}
