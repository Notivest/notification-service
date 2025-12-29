package com.notivest.notificationservice.infrastructure.mapper

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.notivest.notificationservice.application.notification.NotificationOutcome
import com.notivest.notificationservice.application.notification.NotifyAlertCommand
import com.notivest.notificationservice.application.notification.NotifyRecommendationCommand
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.notification.dto.NotificationResponse
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.notification.dto.NotifyAlertRequest
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.notification.dto.NotifyRecommendationRequest
import org.springframework.stereotype.Component

@Component
class NotificationMapper(
    private val objectMapper: ObjectMapper,
) {

    fun toCommand(request: NotifyAlertRequest): NotifyAlertCommand =
        NotifyAlertCommand(
            userId = requireNotNull(request.userId),
            fingerprint = request.fingerprint.trim(),
            occurredAt = requireNotNull(request.occurredAt),
            severity = request.severity.trim(),
            templateKey = request.templateKey.trim(),
            templateData = request.templateData,
        )

    fun toCommand(request: NotifyRecommendationRequest): NotifyRecommendationCommand =
        NotifyRecommendationCommand(
            userId = requireNotNull(request.userId),
            fingerprint = request.fingerprint.trim(),
            occurredAt = requireNotNull(request.occurredAt),
            kind = request.kind.trim(),
            templateKey = request.templateKey.trim(),
            templateData = mergeTemplateData(request),
        )

    fun toResponse(outcome: NotificationOutcome): NotificationResponse =
        if (outcome.accepted) {
            NotificationResponse(
                accepted = true,
                jobId = outcome.jobId,
                scheduledAt = outcome.scheduledAt,
            )
        } else {
            NotificationResponse(
                accepted = false,
                reason = outcome.reason?.name?.lowercase(),
            )
        }

    private fun mergeTemplateData(request: NotifyRecommendationRequest): JsonNode {
        val base = request.templateData
        if (request.extraData.isEmpty()) {
            return base
        }

        if (base.isObject) {
            val merged = (base as ObjectNode).deepCopy()
            request.extraData.forEach { (key, value) ->
                if (!merged.has(key)) {
                    merged.set<JsonNode>(key, objectMapper.valueToTree(value))
                }
            }
            return merged
        }

        if (base.isMissingNode || base.isNull) {
            val merged = objectMapper.createObjectNode()
            request.extraData.forEach { (key, value) ->
                merged.set<JsonNode>(key, objectMapper.valueToTree(value))
            }
            return merged
        }

        return base
    }
}
