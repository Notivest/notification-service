package com.notivest.notificationservice.infrastructure.adapters.out.email

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.notivest.notificationservice.domain.email.EmailTemplateRenderer
import com.notivest.notificationservice.domain.email.RenderedEmailTemplate
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.context.Context
import java.util.Locale

@Component
class ThymeleafEmailTemplateRenderer(
    private val templateEngine: SpringTemplateEngine,
    private val messageSource: MessageSource,
    private val objectMapper: ObjectMapper,
) : EmailTemplateRenderer {

    override fun render(templateKey: String, locale: Locale?, data: JsonNode): RenderedEmailTemplate {
        val templateName = resolveTemplateName(templateKey)
        val variables = enrichTemplateVariables(templateName, toVariables(data))
        val messageLocale = locale ?: Locale.ENGLISH
        val context = Context(messageLocale).apply {
            setVariable("data", variables)
        }

        val recipientName = resolveRecipientName(variables, messageLocale)
        val subjectTarget = resolveSubjectTarget(variables)

        context.setVariable("recipientName", recipientName)
        context.setVariable("subjectTarget", subjectTarget)

        val subject = resolveSubject(templateName, messageLocale, subjectTarget, variables)
        val body = templateEngine.process("email/$templateName", context)

        return RenderedEmailTemplate(subject = subject, body = body)
    }

    private fun resolveTemplateName(templateKey: String): String {
        val normalized = templateKey.trim()
        val candidates =
            listOf(
                normalized,
                normalized.substringBefore('.'),
                normalized.substringBefore('-'),
            ).filter { it.isNotBlank() }
                .distinct()

        return candidates.firstOrNull { templateExists(it) } ?: normalized
    }

    private fun templateExists(templateName: String): Boolean {
        val resourcePath = "templates/email/$templateName.html"
        return javaClass.classLoader?.getResource(resourcePath) != null
    }

    private fun toVariables(data: JsonNode): Map<String, Any?> =
        if (data.isMissingNode || data.isNull) {
            emptyMap()
        } else {
            objectMapper.convertValue(data, object : TypeReference<Map<String, Any?>>() {})
        }

    private fun enrichTemplateVariables(templateName: String, variables: Map<String, Any?>): Map<String, Any?> {
        if (templateName != "alert") {
            return variables
        }

        val enriched = variables.toMutableMap()
        val payload = (variables["payload"] as? Map<*, *>)?.toStringKeyMap().orEmpty()
        val ruleParams = (variables["ruleParams"] as? Map<*, *>)?.toStringKeyMap().orEmpty()

        val eventSnapshotRows = buildRows(payload, payloadKeyOrder, payloadLabelOverrides)
        if (eventSnapshotRows.isNotEmpty()) {
            enriched["eventSnapshotRows"] = eventSnapshotRows
        }

        val ruleSetupRows = buildRows(ruleParams, ruleParamKeyOrder, ruleParamLabelOverrides)
        if (ruleSetupRows.isNotEmpty()) {
            enriched["ruleSetupRows"] = ruleSetupRows
        }

        return enriched
    }

    private fun buildRows(
        source: Map<String, Any?>,
        preferredOrder: List<String>,
        labels: Map<String, String>,
    ): List<LabeledRow> {
        if (source.isEmpty()) {
            return emptyList()
        }

        val rows = mutableListOf<LabeledRow>()
        val consumed = mutableSetOf<String>()

        // Collapse alternative payload keys that represent the same concept.
        if (source.containsKey("lastPrice") || source.containsKey("currentPrice") || source.containsKey("close")) {
            val currentValue = firstNonBlank(source, "lastPrice", "currentPrice", "close")
            if (currentValue != null) {
                rows += LabeledRow("Current value", currentValue)
                consumed += setOf("lastPrice", "currentPrice", "close")
            }
        }

        preferredOrder.forEach { key ->
            if (key in consumed || key in hiddenKeys) {
                return@forEach
            }
            val renderedValue = renderValue(key, source[key]) ?: return@forEach
            rows += LabeledRow(labels[key] ?: toHumanLabel(key), renderedValue)
            consumed += key
        }

        source.forEach { (key, value) ->
            if (key in consumed || key in hiddenKeys) {
                return@forEach
            }
            val renderedValue = renderValue(key, value) ?: return@forEach
            rows += LabeledRow(labels[key] ?: toHumanLabel(key), renderedValue)
        }

        return rows
    }

    private fun firstNonBlank(source: Map<String, Any?>, vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> renderValue(key, source[key]) }

    private fun renderValue(key: String, value: Any?): String? =
        when (value) {
            null -> null
            is String -> normalizeStringValue(key, value)
            is Number -> normalizeNumberValue(key, value)
            is Boolean -> if (value) "Yes" else "No"
            is Iterable<*> -> value.mapNotNull { renderValue(key, it) }.takeIf { it.isNotEmpty() }?.joinToString(", ")
            is Map<*, *> -> value.toStringKeyMap()
                .entries
                .mapNotNull { (entryKey, entryValue) ->
                    renderValue(entryKey, entryValue)?.let { "${toHumanLabel(entryKey)}: $it" }
                }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(", ")
            else -> value.toString()
        }?.takeIf { it.isNotBlank() }

    private fun normalizeStringValue(key: String, raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val enumLike = key in enumLikeKeys || (trimmed.contains('_') && trimmed.uppercase(Locale.US) == trimmed)
        return when {
            key in percentageKeys && !trimmed.endsWith("%") -> "$trimmed%"
            enumLike -> trimmed.replace('_', ' ')
            else -> trimmed
        }
    }

    private fun normalizeNumberValue(key: String, value: Number): String =
        if (key in percentageKeys) "$value%" else value.toString()

    private fun toHumanLabel(key: String): String {
        val spaced = key
            .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
        if (spaced.isEmpty()) return key

        return spaced
            .split(Regex("\\s+"))
            .joinToString(" ") { token ->
                token.lowercase(Locale.US)
                    .replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString() }
            }
    }

    private fun Map<*, *>.toStringKeyMap(): LinkedHashMap<String, Any?> {
        val mapped = linkedMapOf<String, Any?>()
        for ((key, value) in this) {
            if (key is String) {
                mapped[key] = value
            }
        }
        return mapped
    }

    private fun resolveSubject(templateName: String, locale: Locale, subjectTarget: String, variables: Map<String, Any?>): String {
        val messageKey = "email.$templateName.subject"
        val args = subjectArguments(templateName, subjectTarget, variables)
        return messageSource.getMessage(messageKey, args, messageKey, locale) ?: messageKey
    }

    private fun subjectArguments(templateName: String, subjectTarget: String, variables: Map<String, Any?>): Array<Any> =
        when (templateName) {
            "alert" -> arrayOf(nonBlankString(variables["ruleTitle"]) ?: subjectTarget)
            "recommendation" -> arrayOf(subjectTarget)
            else -> arrayOf((variables["title"] ?: subjectTarget).toString())
        }

    private fun resolveRecipientName(variables: Map<String, Any?>, locale: Locale): String {
        val candidate = (variables["recipientName"] as? String)?.takeIf { it.isNotBlank() }
        return candidate ?: messageSource.getMessage("email.common.recipientFallback", null, "there", locale) ?: "there"
    }

    private fun resolveSubjectTarget(variables: Map<String, Any?>): String =
        listOfNotNull(
            nonBlankString(variables["symbol"]),
            nonBlankString(variables["ruleTitle"]),
            nonBlankString(variables["title"]),
        ).firstOrNull()?.toString() ?: "item"

    private fun nonBlankString(value: Any?): String? =
        (value as? String)?.takeIf { it.isNotBlank() }

    private data class LabeledRow(
        val label: String,
        val value: String,
    )

    companion object {
        private val hiddenKeys = setOf(
            "id",
            "eventId",
            "ruleId",
            "alertId",
            "jobId",
            "notificationId",
            "correlationId",
            "traceId",
        )

        private val percentageKeys = setOf(
            "actualPct",
            "deltaPct",
            "drawdownPct",
            "thresholdPct",
            "pct",
            "distancePct",
            "percentile",
        )

        private val enumLikeKeys = setOf(
            "operator",
            "direction",
            "basis",
            "side",
            "pattern",
            "timeframe",
            "kind",
            "severity",
        )

        private val payloadLabelOverrides = linkedMapOf(
            "threshold" to "Threshold",
            "thresholdPct" to "Threshold (%)",
            "delta" to "Difference vs threshold",
            "actualPct" to "Percent change",
            "drawdownPct" to "Drawdown",
            "rsi" to "RSI",
            "operator" to "Condition",
            "lookbackBars" to "Lookback bars",
            "lookback" to "Lookback",
            "basis" to "Basis",
            "currentVolume" to "Current volume",
            "thresholdVolume" to "Volume trigger",
            "barTs" to "Bar time",
            "fromTs" to "From",
            "toTs" to "To",
            "asOf" to "Detected at",
            "note" to "Note",
            "multiplier" to "Multiplier",
            "atrPeriod" to "ATR period",
            "distancePct" to "Distance (%)",
            "distanceATR" to "Distance (ATR)",
            "stddev" to "Standard deviation",
            "side" to "Band side",
            "signal" to "Signal period",
            "targetSymbol" to "Target symbol",
            "daysTo" to "Days to event",
            "lookbackDays" to "Lookback days",
            "lookbackHrs" to "Lookback hours",
        )

        private val ruleParamLabelOverrides = linkedMapOf(
            "operator" to "Condition",
            "value" to "Target value",
            "pct" to "Target change (%)",
            "threshold" to "Threshold",
            "lookback" to "Lookback",
            "lookbackBars" to "Lookback bars",
            "lookbackDays" to "Lookback days",
            "lookbackHrs" to "Lookback hours",
            "fast" to "Fast period",
            "slow" to "Slow period",
            "signal" to "Signal period",
            "period" to "Period",
            "timeframe" to "Timeframe",
            "direction" to "Direction",
            "multiplier" to "Multiplier",
            "percentile" to "Percentile",
            "atrPeriod" to "ATR period",
            "distancePct" to "Distance (%)",
            "distanceATR" to "Distance (ATR)",
            "basis" to "Basis",
            "side" to "Band side",
            "pattern" to "Pattern",
            "targetSymbol" to "Target symbol",
            "daysTo" to "Days to event",
            "score" to "Score",
        )

        private val payloadKeyOrder = listOf(
            "lastPrice",
            "currentPrice",
            "close",
            "threshold",
            "thresholdPct",
            "delta",
            "actualPct",
            "drawdownPct",
            "rsi",
            "operator",
            "lookbackBars",
            "lookback",
            "basis",
            "currentVolume",
            "thresholdVolume",
            "barTs",
            "fromTs",
            "toTs",
            "asOf",
            "note",
            "atrPeriod",
            "multiplier",
            "distancePct",
            "distanceATR",
            "stddev",
            "side",
            "fast",
            "slow",
            "signal",
            "period",
            "pattern",
            "daysTo",
            "score",
            "targetSymbol",
        )

        private val ruleParamKeyOrder = listOf(
            "operator",
            "value",
            "pct",
            "threshold",
            "lookback",
            "lookbackBars",
            "lookbackDays",
            "lookbackHrs",
            "fast",
            "slow",
            "signal",
            "period",
            "timeframe",
            "direction",
            "multiplier",
            "percentile",
            "atrPeriod",
            "distancePct",
            "distanceATR",
            "basis",
            "side",
            "pattern",
            "targetSymbol",
            "daysTo",
            "score",
        )
    }
}
