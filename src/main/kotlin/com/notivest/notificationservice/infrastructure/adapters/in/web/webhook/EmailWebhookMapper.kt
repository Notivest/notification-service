package com.notivest.notificationservice.infrastructure.adapters.`in`.web.webhook

import com.notivest.notificationservice.application.webhook.RegisterEmailEventCommand
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.webhook.dto.EmailWebhookRequest
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EmailWebhookMapper {
    fun toCommand(request: EmailWebhookRequest): RegisterEmailEventCommand =
        RegisterEmailEventCommand(
            eventId = request.eventId ?: UUID.randomUUID(),
            userId = request.userId,
            email = request.email,
            kind = request.kind,
            providerReference = request.providerReference,
            occurredAt = request.occurredAt,
            payload = request.payload,
        )
}
