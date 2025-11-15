package com.notivest.notificationservice.infrastructure.adapters.out.http

import com.fasterxml.jackson.annotation.JsonProperty
import com.notivest.notificationservice.bootstrap.PortfolioServiceProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import java.time.Clock
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

class PortfolioServiceTokenProvider(
    private val restTemplate: RestTemplate,
    private val properties: PortfolioServiceProperties.Auth0CredentialsProperties,
    private val clock: Clock,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val cachedToken = AtomicReference<CachedToken?>()

    fun getAccessToken(): String {
        val now = Instant.now(clock)
        cachedToken.get()?.takeIf { now.isBefore(it.expiresAt) }?.let { return it.value }
        return refreshToken(now)
    }

    private fun refreshToken(now: Instant): String {
        synchronized(this) {
            cachedToken.get()?.takeIf { now.isBefore(it.expiresAt) }?.let { return it.value }

            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val payload =
                mapOf(
                    "client_id" to properties.clientId,
                    "client_secret" to properties.clientSecret,
                    "audience" to properties.audience,
                    "scope" to properties.scope,
                    "grant_type" to CLIENT_CREDENTIALS_GRANT,
                )

            val response =
                runCatching {
                    restTemplate.postForObject(
                        TOKEN_PATH,
                        HttpEntity(payload, headers),
                        Auth0TokenResponse::class.java,
                    )
                }.getOrElse {
                    logger.error("Failed to obtain Auth0 token for portfolio service", it)
                    throw it
                } ?: run {
                    logger.error("Auth0 token response was empty")
                    throw IllegalStateException("Auth0 token response was empty")
                }

            var expiresAt = now.plusSeconds(response.expiresIn).minus(properties.clockSkew)
            if (expiresAt.isBefore(now)) {
                expiresAt = now
            }
            val tokenType = response.tokenType ?: DEFAULT_TOKEN_TYPE
            if (!tokenType.equals(DEFAULT_TOKEN_TYPE, ignoreCase = true)) {
                logger.warn("Unexpected token_type {} received from Auth0, using it as bearer", tokenType)
            }

            return response.accessToken.also {
                cachedToken.set(
                    CachedToken(
                        value = it,
                        expiresAt = expiresAt,
                    ),
                )
            }
        }
    }

    private data class CachedToken(
        val value: String,
        val expiresAt: Instant,
    )

    private data class Auth0TokenResponse(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("expires_in")
        val expiresIn: Long,
        @JsonProperty("token_type")
        val tokenType: String?,
    )

    companion object {
        private const val TOKEN_PATH = "/oauth/token"
        private const val DEFAULT_TOKEN_TYPE = "Bearer"
        private const val CLIENT_CREDENTIALS_GRANT = "client_credentials"
    }
}
