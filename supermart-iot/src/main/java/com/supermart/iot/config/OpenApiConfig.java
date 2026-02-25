package com.supermart.iot.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI supermartOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Supermart IoT Temperature Monitoring API")
                        .description("REST API for the Supermart IoT Temperature Monitoring platform.\n" +
                                "Manages ~3,000 stores, equipment units, IoT devices, real-time telemetry,\n" +
                                "incident tracking, and HVAC technician assignment workflows.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Supermart Engineering")
                                .email("engineering@supermart.com")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token obtained from POST /auth/login"))
                        .addSecuritySchemes("DeviceKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Device-Key")
                                .description("Per-device API key provisioned at device registration")));
    }
}
