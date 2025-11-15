package com.notivest.notificationservice.domain.portfolio

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class PortfolioHolding(
    val portfolioId: UUID,
    val portfolioName: String,
    val symbol: String,
    val quantity: BigDecimal,
    val avgCost: BigDecimal,
    val updatedAt: Instant,
)

interface PortfolioHoldingsQuery {
    fun search(userId: UUID, symbols: List<String>): List<PortfolioHolding>
}
