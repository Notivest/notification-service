package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.emailevent

import com.fasterxml.jackson.databind.ObjectMapper
import com.notivest.notificationservice.domain.email.EmailEvent
import com.notivest.notificationservice.domain.email.EmailEventKind
import org.springframework.stereotype.Component

@Component
class EmailEventEntityMapper(
    private val objectMapper: ObjectMapper,
) {

    fun toEntity(source: EmailEvent): EmailEventEntity =
        EmailEventEntity().apply {
            id = source.id
            userId = source.userId
            email = source.email
            kind = source.kind.name
            providerReference = source.providerReference
            payload = source.payload?.let { objectMapper.writeValueAsString(it) }
            occurredAt = source.occurredAt
            receivedAt = source.receivedAt
        }

    fun toDomain(entity: EmailEventEntity): EmailEvent =
        EmailEvent(
            id = entity.id,
            userId = entity.userId,
            email = entity.email,
            kind = EmailEventKind.valueOf(entity.kind),
            providerReference = entity.providerReference,
            payload = entity.payload?.let { objectMapper.readTree(it) },
            occurredAt = entity.occurredAt,
            receivedAt = entity.receivedAt,
        )
}
