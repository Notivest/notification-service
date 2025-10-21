package com.notivest.notificationservice.domain.emailjob

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.UUID

data class EmailJob(
    val id: UUID,
    val userId: UUID,
    val templateKey: String,
    val templateData: JsonNode,
    val status: EmailJobStatus,
    val attempts: Int,
    val error: String?,
    val scheduledAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun pending(
            userId: UUID,
            templateKey: String,
            templateData: JsonNode,
            scheduledAt: Instant,
            createdAt: Instant,
        ): EmailJob =
            EmailJob(
                id = UUID.randomUUID(),
                userId = userId,
                templateKey = templateKey,
                templateData = templateData,
                status = EmailJobStatus.PENDING,
                attempts = 0,
                error = null,
                scheduledAt = scheduledAt,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
    }
}
