package com.flowcrm.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionConfigurationValidatorTest {

    @Test
    void acceptsStrongSecretsAndExplicitHttpsOrigins() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                "a-unique-production-jwt-secret-with-at-least-256-bits",
                "a-unique-production-database-password-with-sufficient-length",
                "https://crm.example.com,https://www.example.com"
        );

        assertDoesNotThrow(validator::afterPropertiesSet);
    }

    @Test
    void rejectsDefaultJwtSecret() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                "flowcrm-secret-key-for-development-change-in-production-min-256-bits",
                "a-unique-production-database-password-with-sufficient-length",
                "https://crm.example.com"
        );

        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }

    @Test
    void rejectsPlaceholderJwtSecret() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                "REPLACE_WITH_A_UNIQUE_RANDOM_SECRET_OF_AT_LEAST_32_BYTES",
                "a-unique-production-database-password-with-sufficient-length",
                "https://crm.example.com"
        );

        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }

    @Test
    void rejectsLocalCorsOrigins() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                "a-unique-production-jwt-secret-with-at-least-256-bits",
                "a-unique-production-database-password-with-sufficient-length",
                "http://localhost:3001"
        );

        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }

    @Test
    void rejectsWeakDatabasePasswords() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                "a-unique-production-jwt-secret-with-at-least-256-bits",
                "flowcrm",
                "https://crm.example.com"
        );

        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }
}
