package com.notivest.notificationservice.domain.contact

import java.time.Instant
import java.util.Locale
import java.util.UUID

data class UserContact(
    val userId: UUID,
    val primaryEmail: String,
    val emailStatus: EmailStatus,
    val locale: Locale?,
    val channels: Map<String, Boolean>,
    val quietHours: QuietHours?,
    val version: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)
