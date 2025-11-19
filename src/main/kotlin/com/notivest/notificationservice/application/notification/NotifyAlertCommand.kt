package com.notivest.notificationservice.application.notification

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.UUID

data class NotifyAlertCommand(
    val userId: UUID,
    val fingerprint: String,
    val occurredAt: Instant,
    val severity: String,
    val templateKey: String,
    val templateData: JsonNode,
)
