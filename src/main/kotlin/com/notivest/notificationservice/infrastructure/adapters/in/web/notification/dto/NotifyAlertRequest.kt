package com.notivest.notificationservice.infrastructure.adapters.`in`.web.notification.dto

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class NotifyAlertRequest(
    @field:NotNull
    val userId: UUID?,

    @field:NotBlank
    @field:Size(max = 128)
    val fingerprint: String,

    @field:NotNull
    val occurredAt: Instant?,

    @field:NotBlank
    @field:Size(max = 32)
    val severity: String,

    @field:NotBlank
    @field:Size(max = 64)
    val templateKey: String,

    @field:NotNull
    val templateData: JsonNode = NullNode.instance,
)
