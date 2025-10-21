package com.notivest.notificationservice.application.webhook

import com.fasterxml.jackson.databind.JsonNode
import com.notivest.notificationservice.domain.email.EmailEventKind
import java.time.Instant
import java.util.UUID

data class RegisterEmailEventCommand(
    val eventId: UUID = UUID.randomUUID(),
    val userId: UUID?,
    val email: String,
    val kind: EmailEventKind,
    val providerReference: String?,
    val occurredAt: Instant,
    val payload: JsonNode?,
)
