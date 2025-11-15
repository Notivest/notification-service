package com.notivest.notificationservice.infrastructure.adapters.out.http

import com.notivest.notificationservice.domain.portfolio.PortfolioHolding
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class PortfolioServiceClientTest {

    private lateinit var restTemplate: RestTemplate
    private lateinit var mockServer: MockRestServiceServer
    private lateinit var client: PortfolioServiceClient

    private val baseUrl = "https://portfolio.test"
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        restTemplate = RestTemplateBuilder().rootUri(baseUrl).build()
        mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build()
        client = PortfolioServiceClient(restTemplate)
    }

    @Test
    fun `returns holdings when service responds with data`() {
        val responseBody =
            """
            [
              {
                "portfolioId": "5a69d5d4-2ce9-4657-9444-9ff5f5b0b131",
                "portfolioName": "Cartera USD",
                "symbol": "AAPL",
                "quantity": 12.5,
                "avgCost": 188.12,
                "updatedAt": "2024-06-01T10:15:00Z"
              }
            ]
            """.trimIndent()

        mockServer.expect(requestTo("$baseUrl/internal/v1/holdings/search"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("\$.userId").value(userId.toString()))
            .andExpect(jsonPath("\$.symbols[0]").value("AAPL"))
            .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON))

        val result = client.search(userId, listOf("aapl"))

        assertThat(result)
            .hasSize(1)
            .first()
            .usingRecursiveComparison()
            .isEqualTo(
                PortfolioHolding(
                    portfolioId = UUID.fromString("5a69d5d4-2ce9-4657-9444-9ff5f5b0b131"),
                    portfolioName = "Cartera USD",
                    symbol = "AAPL",
                    quantity = BigDecimal("12.5"),
                    avgCost = BigDecimal("188.12"),
                    updatedAt = Instant.parse("2024-06-01T10:15:00Z"),
                ),
            )
        mockServer.verify()
    }

    @Test
    fun `returns empty list when service responds 404`() {
        mockServer.expect(requestTo("$baseUrl/internal/v1/holdings/search"))
            .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND))

        val result = client.search(userId, listOf("AAPL"))

        assertThat(result).isEmpty()
        mockServer.verify()
    }
}
