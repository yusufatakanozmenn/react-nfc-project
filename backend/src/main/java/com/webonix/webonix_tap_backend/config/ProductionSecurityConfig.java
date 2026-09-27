package com.webonix.webonix_tap_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import java.net.URI;

@Configuration
@Profile("production")
public class ProductionSecurityConfig {
    public ProductionSecurityConfig(Environment env) {
        if (!env.getProperty("app.cookies.secure", Boolean.class, false)) {
            throw new IllegalStateException("Production requires Secure cookies.");
        }
        String origins = env.getRequiredProperty("app.cors.allowed-origins");
        for (String origin : origins.split(",", -1)) {
            URI uri = URI.create(origin.trim());
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null || !uri.getPath().isEmpty()) {
                throw new IllegalStateException("Production CORS origins must be exact HTTPS origins.");
            }
        }
        if (!"validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto"))) {
            throw new IllegalStateException("Production requires managed migrations and ddl-auto=validate.");
        }
        if ("root".equalsIgnoreCase(env.getRequiredProperty("spring.datasource.username"))) {
            throw new IllegalStateException("Production requires a dedicated database account.");
        }
    }
}
