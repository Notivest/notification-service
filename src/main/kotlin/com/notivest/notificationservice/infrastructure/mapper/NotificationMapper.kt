package com.notivest.notificationservice.infrastructure.mapper

import com.notivest.notificationservice.application.notification.NotificationOutcome
import com.notivest.notificationservice.application.notification.NotifyAlertCommand
import com.notivest.notificationservice.application.notification.NotifyRecommendationCommand
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.notification.dto.NotificationResponse
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.notification.dto.NotifyAlertRequest
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.notification.dto.NotifyRecommendationRequest
import org.springframework.stereotype.Component

@Component
class NotificationMapper {

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
            templateData = request.templateData,
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
}
