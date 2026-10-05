package com.flowcrm.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
@Profile("prod")
public class ProductionConfigurationValidator implements InitializingBean {

    private static final String DEVELOPMENT_JWT_SECRET =
            "flowcrm-secret-key-for-development-change-in-production-min-256-bits";

    private final String jwtSecret;
    private final String databasePassword;
    private final String allowedOrigins;

    public ProductionConfigurationValidator(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${spring.datasource.password}") String databasePassword,
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        this.jwtSecret = jwtSecret;
        this.databasePassword = databasePassword;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void afterPropertiesSet() {
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32
                || DEVELOPMENT_JWT_SECRET.equals(jwtSecret)
                || "change-me-in-local-env-use-at-least-256-bits".equals(jwtSecret)
                || jwtSecret.startsWith("REPLACE_WITH_")) {
            throw new IllegalStateException("Production requires a unique JWT_SECRET with at least 256 bits");
        }
        if (databasePassword.length() < 24
                || "flowcrm".equals(databasePassword)
                || databasePassword.startsWith("REPLACE_WITH_")) {
            throw new IllegalStateException("Production requires a strong, unique database password");
        }

        boolean hasInvalidOrigin = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .anyMatch(origin -> !isSecureOrigin(origin));
        if (hasInvalidOrigin) {
            throw new IllegalStateException("Production CORS origins must be explicit HTTPS origins");
        }
    }

    private boolean isSecureOrigin(String value) {
        try {
            URI origin = URI.create(value);
            return "https".equalsIgnoreCase(origin.getScheme())
                    && origin.getHost() != null
                    && origin.getUserInfo() == null
                    && origin.getPath().isEmpty()
                    && origin.getQuery() == null
                    && origin.getFragment() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
