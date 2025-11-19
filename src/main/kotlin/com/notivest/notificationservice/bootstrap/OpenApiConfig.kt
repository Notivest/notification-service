package com.notivest.notificationservice.bootstrap

import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun notificationOpenApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Notification Service API")
                    .version("1.0.0")
                    .description("HTTP endpoints for notifications, contact management, and webhooks")
                    .contact(
                        Contact()
                            .name("Notification Team")
                            .email("notifications@example.com"),
                    )
                    .license(
                        License()
                            .name("Apache 2.0")
                            .url("https://www.apache.org/licenses/LICENSE-2.0"),
                    ),
            )
            .externalDocs(
                ExternalDocumentation()
                    .description("Project documentation")
                    .url("https://docs.example.com/notification-service"),
            )
}
