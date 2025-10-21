package com.notivest.notificationservice.application.webhook

import com.notivest.notificationservice.domain.contact.EmailStatus
import com.notivest.notificationservice.domain.contact.port.UserContactRepository
import com.notivest.notificationservice.domain.email.EmailEvent
import com.notivest.notificationservice.domain.email.EmailEventKind
import com.notivest.notificationservice.domain.email.port.EmailEventRepository
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class EmailWebhookApplicationService(
    private val emailEventRepository: EmailEventRepository,
    private val userContactRepository: UserContactRepository,
    private val clock: Clock,
) : RegisterEmailEventUseCase {

    override fun register(command: RegisterEmailEventCommand) {
        val receivedAt = Instant.now(clock)
        val event =
            EmailEvent(
                id = command.eventId,
                userId = command.userId,
                email = command.email,
                kind = command.kind,
                providerReference = command.providerReference,
                payload = command.payload,
                occurredAt = command.occurredAt,
                receivedAt = receivedAt,
            )
        emailEventRepository.save(event)

        val statusToApply = targetStatus(command.kind)
        if (statusToApply != null && command.userId != null) {
            val contact = userContactRepository.findByUserId(command.userId)
            if (contact != null && contact.emailStatus != statusToApply) {
                val updated =
                    contact.copy(
                        emailStatus = statusToApply,
                        updatedAt = receivedAt,
                        version = contact.version + 1,
                    )
                userContactRepository.save(updated)
            }
        }
    }

    private fun targetStatus(kind: EmailEventKind): EmailStatus? =
        when (kind) {
            EmailEventKind.BOUNCE -> EmailStatus.BOUNCED
            EmailEventKind.COMPLAINT -> EmailStatus.BOUNCED
            EmailEventKind.UNSUBSCRIBE -> EmailStatus.UNSUB
            EmailEventKind.DELIVERED -> EmailStatus.VERIFIED
            else -> null
        }
}
