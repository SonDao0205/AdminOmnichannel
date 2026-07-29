package com.admin.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI adminOpenApi(AdminSecurityProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title("OmnichannelPOS Platform Admin API")
                        .version("1.0.0")
                        .description("Platform-owner login and tenant provisioning API"))
                .components(new Components().addSecuritySchemes(
                        "adminSession",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(properties.getCookieName())
                                .description("Opaque HttpOnly platform-owner session cookie")));
    }
}
