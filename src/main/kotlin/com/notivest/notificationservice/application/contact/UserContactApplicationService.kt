package com.notivest.notificationservice.application.contact

import com.notivest.notificationservice.domain.contact.UserContact
import com.notivest.notificationservice.domain.contact.port.UserContactRepository
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class UserContactApplicationService(
    private val repository: UserContactRepository,
    private val clock: Clock,
) : GetUserContactQuery, UpsertUserContactUseCase {

    override fun get(userId: UUID): UserContact? =
        repository.findByUserId(userId)

    override fun upsert(command: UpsertUserContactCommand): UserContact {
        val existing = repository.findByUserId(command.userId)
        val now = Instant.now(clock)
        val nextVersion = existing?.version?.plus(1) ?: 0L
        val channels = command.channels ?: existing?.channels ?: DEFAULT_CHANNELS

        val contact =
            UserContact(
                userId = command.userId,
                primaryEmail = command.primaryEmail,
                emailStatus = command.emailStatus,
                locale = command.locale,
                channels = channels,
                quietHours = command.quietHours,
                version = nextVersion,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )

        return repository.save(contact)
    }

    private companion object {
        val DEFAULT_CHANNELS = mapOf("email" to true)
    }
}
