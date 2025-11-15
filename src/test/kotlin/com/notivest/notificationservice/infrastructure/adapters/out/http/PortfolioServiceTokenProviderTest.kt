package com.notivest.notificationservice.infrastructure.adapters.out.http

import com.notivest.notificationservice.bootstrap.PortfolioServiceProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicReference

class PortfolioServiceTokenProviderTest {

    private val properties =
        PortfolioServiceProperties.Auth0CredentialsProperties(
            domain = "https://auth0.example.com",
            clientId = "client-id",
            clientSecret = "client-secret",
            audience = "https://api.notivest/portfolio",
            scope = "portfolio:read:user-context",
            connectTimeout = Duration.ofMillis(100),
            readTimeout = Duration.ofSeconds(1),
            clockSkew = Duration.ofSeconds(30),
        )

    private lateinit var restTemplateBuilder: RestTemplateBuilder
    private lateinit var mockServer: MockRestServiceServer
    private lateinit var clock: MutableClock
    private lateinit var tokenProvider: PortfolioServiceTokenProvider

    @BeforeEach
    fun setUp() {
        clock = MutableClock(Instant.parse("2024-01-01T00:00:00Z"))

        restTemplateBuilder = RestTemplateBuilder().rootUri(properties.domain)
        val restTemplate = restTemplateBuilder.build()
        mockServer = MockRestServiceServer.bindTo(restTemplate).build()

        tokenProvider = PortfolioServiceTokenProvider(restTemplate, properties, clock)
    }

    @Test
    fun `obtains token once and caches until near expiry`() {
        mockServer
            .expect(requestTo("https://auth0.example.com/oauth/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.client_id").value("client-id"))
            .andExpect(jsonPath("$.client_secret").value("client-secret"))
            .andExpect(jsonPath("$.audience").value("https://api.notivest/portfolio"))
            .andExpect(jsonPath("$.scope").value("portfolio:read:user-context"))
            .andRespond(
                withSuccess(
                    """{"access_token":"token-1","expires_in":3600,"token_type":"Bearer"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val firstToken = tokenProvider.getAccessToken()
        assertThat(firstToken).isEqualTo("token-1")

        val secondToken = tokenProvider.getAccessToken()
        assertThat(secondToken).isEqualTo("token-1")

        mockServer.verify()
    }

    @Test
    fun `refreshes token after expiry window`() {
        mockServer
            .expect(requestTo("https://auth0.example.com/oauth/token"))
            .andRespond(
                withSuccess(
                    """{"access_token":"token-1","expires_in":90,"token_type":"Bearer"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        mockServer
            .expect(requestTo("https://auth0.example.com/oauth/token"))
            .andRespond(
                withSuccess(
                    """{"access_token":"token-2","expires_in":90,"token_type":"Bearer"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val initialToken = tokenProvider.getAccessToken()
        assertThat(initialToken).isEqualTo("token-1")

        clock.advanceBy(Duration.ofSeconds(100))

        val refreshedToken = tokenProvider.getAccessToken()
        assertThat(refreshedToken).isEqualTo("token-2")

        mockServer.verify()
    }

    private class MutableClock(initialInstant: Instant) : Clock() {
        private val instantRef = AtomicReference(initialInstant)

        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = instantRef.get()

        fun advanceBy(duration: Duration) {
            instantRef.updateAndGet { it.plus(duration) }
        }
    }
}
