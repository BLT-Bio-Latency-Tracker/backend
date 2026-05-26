package com.medilux.blt.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun openAPI(): OpenAPI {
        val securitySchemeName = "bearerAuth"
        val securityScheme =
            SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")

        return OpenAPI()
            .info(
                Info()
                    .title("BLT (Bio-Latency Tracker) API")
                    .version("1.0.0"),
            ).components(
                Components()
                    .addSecuritySchemes(securitySchemeName, securityScheme),
            )
    }
}
