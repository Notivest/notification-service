package com.notivest.notificationservice.application.contact

import com.notivest.notificationservice.domain.contact.EmailStatus
import com.notivest.notificationservice.domain.contact.QuietHours
import java.util.Locale
import java.util.UUID

data class UpsertUserContactCommand(
    val userId: UUID,
    val primaryEmail: String,
    val emailStatus: EmailStatus,
    val locale: Locale?,
    val channels: Map<String, Boolean>?,
    val quietHours: QuietHours?,
)
