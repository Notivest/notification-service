package com.notivest.notificationservice.application.contact

import com.notivest.notificationservice.domain.contact.EmailStatus
import com.notivest.notificationservice.domain.contact.QuietHours
import com.notivest.notificationservice.domain.contact.UserContact
import com.notivest.notificationservice.domain.contact.port.UserContactRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID

class UserContactApplicationServiceTest {

    private val repository: UserContactRepository = mockk(relaxed = true)
    private val fixedInstant = Instant.parse("2024-06-01T10:15:30Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val service = UserContactApplicationService(repository, clock)

    @Test
    fun `upsert creates new contact when none exists`() {
        val userId = UUID.randomUUID()
        val command =
            UpsertUserContactCommand(
                userId = userId,
                primaryEmail = "user@example.com",
                emailStatus = EmailStatus.VERIFIED,
                locale = Locale("en", "US"),
                channels = mapOf("email" to true, "sms" to false),
                quietHours =
                    QuietHours(
                        start = LocalTime.of(22, 0),
                        end = LocalTime.of(7, 0),
                        timezone = ZoneId.of("UTC"),
                    ),
            )

        every { repository.findByUserId(userId) } returns null
        every { repository.save(any()) } answers { firstArg() }

        val result = service.upsert(command)

        assertThat(result.version).isEqualTo(0L)
        assertThat(result.createdAt).isEqualTo(fixedInstant)
        assertThat(result.updatedAt).isEqualTo(fixedInstant)
        assertThat(result.primaryEmail).isEqualTo(command.primaryEmail)
        verify {
            repository.save(
                match {
                    it.userId == userId &&
                        it.version == 0L &&
                        it.createdAt == fixedInstant &&
                        it.updatedAt == fixedInstant &&
                        it.channels == command.channels &&
                        it.quietHours == command.quietHours
                },
            )
        }
    }

    @Test
    fun `upsert updates existing contact preserving created at and incrementing version`() {
        val userId = UUID.randomUUID()
        val existingCreatedAt = Instant.parse("2024-01-01T00:00:00Z")
        val existingContact =
            UserContact(
                userId = userId,
                primaryEmail = "old@example.com",
                emailStatus = EmailStatus.UNVERIFIED,
                locale = Locale.FRANCE,
                channels = mapOf("email" to true),
                quietHours = null,
                version = 2L,
                createdAt = existingCreatedAt,
                updatedAt = Instant.parse("2024-05-01T00:00:00Z"),
            )
        val command =
            UpsertUserContactCommand(
                userId = userId,
                primaryEmail = "new@example.com",
                emailStatus = EmailStatus.VERIFIED,
                locale = Locale.UK,
                channels = mapOf("email" to true, "push" to true),
                quietHours = null,
            )

        every { repository.findByUserId(userId) } returns existingContact
        every { repository.save(any()) } answers { firstArg() }

        val result = service.upsert(command)

        assertThat(result.version).isEqualTo(3L)
        assertThat(result.createdAt).isEqualTo(existingCreatedAt)
        assertThat(result.updatedAt).isEqualTo(fixedInstant)
        verify {
            repository.save(
                match {
                    it.version == 3L &&
                        it.createdAt == existingCreatedAt &&
                        it.updatedAt == fixedInstant &&
                        it.primaryEmail == command.primaryEmail
                },
            )
        }
    }
}
