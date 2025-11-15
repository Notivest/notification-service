package com.notivest.notificationservice.application.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.notivest.notificationservice.domain.portfolio.PortfolioHolding
import com.notivest.notificationservice.domain.portfolio.PortfolioHoldingsQuery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class AlertTemplateDataEnricherTest {

    private val portfolioHoldingsQuery: PortfolioHoldingsQuery = mockk()
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private lateinit var enricher: AlertTemplateDataEnricher

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        enricher = AlertTemplateDataEnricher(portfolioHoldingsQuery, objectMapper)
    }

    @Test
    fun `returns original data when symbol is missing`() {
        val templateData = objectMapper.createObjectNode().put("severity", "WARN")

        val result = enricher.enrich(userId, templateData)

        assertThat(result).isSameAs(templateData)
        verify(exactly = 0) { portfolioHoldingsQuery.search(any(), any()) }
    }

    @Test
    fun `enriches template data with holdings`() {
        val holding =
            PortfolioHolding(
                portfolioId = UUID.randomUUID(),
                portfolioName = "Cartera USD",
                symbol = "AAPL",
                quantity = BigDecimal("12.5"),
                avgCost = BigDecimal("188.12"),
                updatedAt = Instant.parse("2024-06-01T10:15:00Z"),
            )
        every { portfolioHoldingsQuery.search(userId, listOf("AAPL")) } returns listOf(holding)
        val templateData = objectMapper.createObjectNode().put("symbol", "AAPL")

        val result = enricher.enrich(userId, templateData)

        assertThat(result).isNotSameAs(templateData)
        assertThat(result.path("symbol").asText()).isEqualTo("AAPL")
        val holdings = result.path("holdings")
        assertThat(holdings.isArray).isTrue()
        assertThat(holdings.size()).isEqualTo(1)
        val first = holdings.get(0)
        assertThat(first.path("portfolioName").asText()).isEqualTo("Cartera USD")
        assertThat(first.path("quantity").asDouble()).isEqualTo(12.5)
    }

    @Test
    fun `includes empty holdings array when lookup fails`() {
        every { portfolioHoldingsQuery.search(userId, listOf("AAPL")) } throws RuntimeException("boom")
        val templateData = objectMapper.createObjectNode().put("symbol", "AAPL")

        val result = enricher.enrich(userId, templateData)

        val holdings = result.path("holdings")
        assertThat(holdings.isArray).isTrue()
        assertThat(holdings.size()).isZero()
        assertThat(result.path("symbol").asText()).isEqualTo("AAPL")
    }

    @Test
    fun `normalizes symbol before lookup`() {
        every { portfolioHoldingsQuery.search(userId, listOf("AAPL")) } returns emptyList()
        val templateData = objectMapper.createObjectNode().put("symbol", " aapl ")

        val result = enricher.enrich(userId, templateData)

        verify { portfolioHoldingsQuery.search(userId, listOf("AAPL")) }
        assertThat(result.path("symbol").asText()).isEqualTo("AAPL")
    }
}
