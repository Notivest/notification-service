package com.notivest.notificationservice.infrastructure.adapters.`in`.web.notification.dto

import java.time.Instant
import java.util.UUID

data class NotificationResponse(
    val accepted: Boolean,
    val jobId: UUID? = null,
    val scheduledAt: Instant? = null,
    val reason: String? = null,
)
