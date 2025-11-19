package com.notivest.notificationservice.infrastructure.adapters.`in`.web.webhook.dto

import com.fasterxml.jackson.databind.JsonNode
import com.notivest.notificationservice.domain.email.EmailEventKind
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class EmailWebhookRequest(
    val eventId: UUID? = null,
    val userId: UUID? = null,

    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotNull
    val kind: EmailEventKind,

    val providerReference: String? = null,

    @field:NotNull
    val occurredAt: Instant,

    val payload: JsonNode? = null,
)
