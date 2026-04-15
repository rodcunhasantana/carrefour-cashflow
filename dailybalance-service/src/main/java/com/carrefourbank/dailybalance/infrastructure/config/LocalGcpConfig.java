package com.carrefourbank.dailybalance.infrastructure.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Fornece credenciais no-op para o perfil dev.
 * Quando usando o emulador do Pub/Sub, nenhuma credencial GCP real é necessária.
 * Sem este bean, o GcpContextAutoConfiguration tenta carregar Application Default
 * Credentials e loga um WARN quando não as encontra.
 */
@Configuration
@Profile("dev")
class LocalGcpConfig {

    @Bean
    @Primary
    CredentialsProvider googleCredentials() {
        return NoCredentialsProvider.create();
    }
}
