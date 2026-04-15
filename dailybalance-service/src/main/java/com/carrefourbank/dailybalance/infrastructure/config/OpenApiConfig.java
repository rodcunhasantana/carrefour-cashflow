package com.carrefourbank.dailybalance.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dailyBalanceServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Daily Balance Service API")
                        .description("Serviço de saldo diário consolidado — consolida lançamentos e gerencia fechamento de períodos.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Carrefour Bank Platform")
                                .email("platform@carrefourbank.com")));
    }
}
