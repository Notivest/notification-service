package com.notivest.notificationservice.application.notification

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.notivest.notificationservice.domain.portfolio.PortfolioHoldingsQuery
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.Locale

@Component
class AlertTemplateDataEnricher(
    private val portfolioHoldingsQuery: PortfolioHoldingsQuery,
    private val objectMapper: ObjectMapper,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun enrich(userId: UUID, templateData: JsonNode): JsonNode {
        val symbol = extractSymbol(templateData)?.ifBlank { null }?.uppercase(Locale.US) ?: return templateData

        val baseNode = ensureObjectNode(templateData).apply { put("symbol", symbol) }

        val holdings =
            runCatching { portfolioHoldingsQuery.search(userId, listOf(symbol)) }
                .onFailure { logger.warn("Failed to fetch holdings for user {} and symbol {}: {}", userId, symbol, it.message) }
                .getOrNull()
                ?: return baseNode.apply { putArrayIfMissing("holdings") }

        val holdingsNode = objectMapper.valueToTree<ArrayNode>(holdings)
        baseNode.set<ArrayNode>("holdings", holdingsNode)
        return baseNode
    }

    private fun extractSymbol(templateData: JsonNode): String? =
        templateData.path("symbol")
            .takeIf { !it.isMissingNode && !it.isNull }
            ?.asText()
            ?.trim()

    private fun ensureObjectNode(node: JsonNode): ObjectNode =
        when {
            node.isObject -> node.deepCopy<ObjectNode>()
            node.isMissingNode || node.isNull -> objectMapper.createObjectNode()
            else -> objectMapper.convertValue(node, ObjectNode::class.java)
        }

    private fun ObjectNode.putArrayIfMissing(fieldName: String) {
        if (!has(fieldName)) {
            set<ArrayNode>(fieldName, objectMapper.createArrayNode())
        }
    }
}
