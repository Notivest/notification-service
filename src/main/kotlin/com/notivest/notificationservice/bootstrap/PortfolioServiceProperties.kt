package com.notivest.notificationservice.bootstrap

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "portfolio.service")
data class PortfolioServiceProperties(
    val baseUrl: String,
    val connectTimeout: Duration = Duration.ofMillis(500),
    val readTimeout: Duration = Duration.ofSeconds(3),
    val auth: Auth0CredentialsProperties,
) {

    data class Auth0CredentialsProperties(
        val domain: String,
        val clientId: String,
        val clientSecret: String,
        val audience: String,
        val scope: String = "portfolio:read:user-context",
        val connectTimeout: Duration = Duration.ofMillis(500),
        val readTimeout: Duration = Duration.ofSeconds(3),
        val clockSkew: Duration = Duration.ofSeconds(60),
    )
}
