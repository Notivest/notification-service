package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.emailjob

import com.fasterxml.jackson.databind.ObjectMapper
import com.notivest.notificationservice.domain.emailjob.EmailJob
import com.notivest.notificationservice.domain.emailjob.EmailJobStatus
import org.springframework.stereotype.Component

@Component
class EmailJobEntityMapper(
    private val objectMapper: ObjectMapper,
) {

    fun toEntity(source: EmailJob): EmailJobEntity =
        EmailJobEntity().apply {
            id = source.id
            userId = source.userId
            templateKey = source.templateKey
            templateJson = objectMapper.writeValueAsString(source.templateData)
            status = source.status.name
            attempts = source.attempts
            error = source.error
            scheduledAt = source.scheduledAt
            createdAt = source.createdAt
            updatedAt = source.updatedAt
        }

    fun toDomain(entity: EmailJobEntity): EmailJob =
        EmailJob(
            id = entity.id,
            userId = entity.userId,
            templateKey = entity.templateKey,
            templateData = objectMapper.readTree(entity.templateJson),
            status = java.lang.Enum.valueOf(EmailJobStatus::class.java, entity.status),
            attempts = entity.attempts,
            error = entity.error,
            scheduledAt = entity.scheduledAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
}
