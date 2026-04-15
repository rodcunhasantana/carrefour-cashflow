package com.carrefourbank.transaction.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transactionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction Service API")
                        .description("Serviço de lançamentos financeiros — registra créditos, débitos e estornos.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Carrefour Bank Platform")
                                .email("platform@carrefourbank.com")));
    }
}
