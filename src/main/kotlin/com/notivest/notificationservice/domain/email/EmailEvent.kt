package com.notivest.notificationservice.domain.email

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.UUID

enum class EmailEventKind {
    DELIVERED,
    OPEN,
    CLICK,
    BOUNCE,
    COMPLAINT,
    UNSUBSCRIBE,
    OTHER,
}

data class EmailEvent(
    val id: UUID,
    val userId: UUID?,
    val email: String,
    val kind: EmailEventKind,
    val providerReference: String?,
    val payload: JsonNode?,
    val occurredAt: Instant,
    val receivedAt: Instant,
)
