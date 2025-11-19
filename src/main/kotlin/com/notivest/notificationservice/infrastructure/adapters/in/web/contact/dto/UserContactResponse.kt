package com.notivest.notificationservice.infrastructure.adapters.`in`.web.contact.dto

import com.notivest.notificationservice.domain.contact.EmailStatus
import java.time.Instant
import java.util.UUID

data class UserContactResponse(
    val userId: UUID,
    val primaryEmail: String,
    val emailStatus: EmailStatus,
    val locale: String?,
    val quietHours: QuietHoursDto?,
    val version: Long,
    val updatedAt: Instant,
    val createdAt: Instant,
)
