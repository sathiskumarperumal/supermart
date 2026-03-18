package com.supermart.iot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Centralised CORS configuration for all REST API endpoints.
 *
 * <p>Allowed origins are externalised to {@code app.cors.allowed-origins} in
 * {@code application.properties} and bound to the {@code APP_CORS_ALLOWED_ORIGINS}
 * environment variable so that the frontend URL is never hardcoded in source.
 * CORS rules are scoped to {@code /api/**} only, per OWASP A05 guidance.</p>
 *
 * <p>{@code allowCredentials} is set to {@code false} because this API uses
 * JWT Bearer tokens sent via the {@code Authorization} header — no browser
 * cookie sharing is required.</p>
 *
 * <p>Wired into {@link SecurityConfig} via {@link CorsConfigurationSource}.</p>
 */
@Configuration
@Slf4j
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    /**
     * Registers CORS rules for all routes under {@code /api/**}.
     *
     * <p>Allowed methods cover the standard REST verbs. Allowed headers include
     * {@code Authorization} and {@code Content-Type} for JWT-authenticated calls.
     * Pre-flight responses are cached for 3600 seconds (1 hour).</p>
     *
     * @return a configured {@link CorsConfigurationSource} applied to {@code /api/**}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setExposedHeaders(List.of("X-Total-Count", "Location"));
        config.setAllowCredentials(false); // JWT via Authorization header — no cookies
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        log.info("CORS configured for origins: {}", allowedOrigins);
        return source;
    }
}
