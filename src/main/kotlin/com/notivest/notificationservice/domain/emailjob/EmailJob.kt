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

    fun markSent(processedAt: Instant): EmailJob =
        copy(
            status = EmailJobStatus.SENT,
            attempts = attempts + 1,
            error = null,
            updatedAt = processedAt,
        )

    fun markFailed(processedAt: Instant, message: String): EmailJob =
        copy(
            status = EmailJobStatus.FAILED,
            attempts = attempts + 1,
            error = sanitizeError(message),
            updatedAt = processedAt,
        )

    companion object {
        private const val MAX_ERROR_LENGTH = 255

        private fun sanitizeError(message: String): String {
            val normalized = message.trim()
            return if (normalized.length <= MAX_ERROR_LENGTH) normalized else normalized.take(MAX_ERROR_LENGTH)
        }

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
