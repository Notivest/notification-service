package com.notivest.notificationservice.infrastructure.adapters.out.http

import com.notivest.notificationservice.domain.portfolio.PortfolioHolding
import com.notivest.notificationservice.domain.portfolio.PortfolioHoldingsQuery
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.Locale
import java.util.UUID

@Component
class PortfolioServiceClient(
    @Qualifier("portfolioServiceRestTemplate") private val restTemplate: RestTemplate,
) : PortfolioHoldingsQuery {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun search(userId: UUID, symbols: List<String>): List<PortfolioHolding> {
        val normalizedSymbols =
            symbols.mapNotNull { it.trim().takeIf(String::isNotEmpty)?.uppercase(Locale.US) }
        if (normalizedSymbols.isEmpty()) {
            logger.debug("Skipping portfolio lookup for user {} because symbol list is empty after normalization", userId)
            return emptyList()
        }

        val request = HoldingsSearchRequest(userId, normalizedSymbols)
        return try {
            val response =
                restTemplate.postForEntity(
                    SEARCH_PATH,
                    request,
                    Array<HoldingsSearchResponse>::class.java,
                )
            when (response.statusCode) {
                HttpStatus.OK -> response.body?.map { it.toDomain() } ?: emptyList()
                HttpStatus.NOT_FOUND -> emptyList()
                else -> {
                    logger.warn(
                        "Unexpected status {} while fetching holdings for user {} and symbols {}",
                        response.statusCode,
                        userId,
                        normalizedSymbols,
                    )
                    emptyList()
                }
            }
        } catch (ex: HttpClientErrorException.NotFound) {
            emptyList()
        }
    }

    private data class HoldingsSearchRequest(
        val userId: UUID,
        val symbols: List<String>,
    )

    private data class HoldingsSearchResponse(
        val portfolioId: UUID,
        val portfolioName: String,
        val symbol: String,
        val quantity: java.math.BigDecimal,
        val avgCost: java.math.BigDecimal,
        val updatedAt: java.time.Instant,
    ) {
        fun toDomain(): PortfolioHolding =
            PortfolioHolding(
                portfolioId = portfolioId,
                portfolioName = portfolioName,
                symbol = symbol,
                quantity = quantity,
                avgCost = avgCost,
                updatedAt = updatedAt,
            )
    }

    companion object {
        private const val SEARCH_PATH = "/internal/v1/holdings/search"
    }
}
