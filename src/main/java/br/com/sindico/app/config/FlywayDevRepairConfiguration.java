package br.com.sindico.app.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Em dev com H2 arquivo, editar um script Flyway ja aplicado invalida checksum e o contexto nao sobe
 * (FlywayValidateException). Com {@code app.flyway.repair-before-migrate=true} (ou env
 * {@code APP_FLYWAY_REPAIR=true}), roda {@code repair()} uma vez para alinhar o historico ao disco.
 * Nao usar em prod.
 */
@Configuration
@Profile("dev")
@ConditionalOnProperty(prefix = "app.flyway", name = "repair-before-migrate", havingValue = "true")
public class FlywayDevRepairConfiguration {

    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
