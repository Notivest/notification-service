package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.usercontact

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.notivest.notificationservice.domain.contact.EmailStatus
import com.notivest.notificationservice.domain.contact.QuietHours
import com.notivest.notificationservice.domain.contact.UserContact
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

class UserContactEntityMapperTest {

    private val objectMapper =
        ObjectMapper()
            .findAndRegisterModules()
            .registerKotlinModule()

    private val mapper = UserContactEntityMapper(objectMapper)

    @Test
    fun `toEntity serializes fields correctly`() {
        val contact =
            UserContact(
                userId = UUID.randomUUID(),
                primaryEmail = "user@example.com",
                emailStatus = EmailStatus.VERIFIED,
                locale = Locale("es", "AR"),
                channels = mapOf("email" to true, "push" to false),
                quietHours =
                    QuietHours(
                        start = LocalTime.of(21, 30),
                        end = LocalTime.of(7, 45),
                        timezone = ZoneId.of("America/Buenos_Aires"),
                    ),
                version = 3,
                createdAt = Instant.parse("2024-05-01T12:00:00Z"),
                updatedAt = Instant.parse("2024-05-02T12:00:00Z"),
            )

        val entity = mapper.toEntity(contact)

        assertThat(entity.userId).isEqualTo(contact.userId)
        assertThat(entity.primaryEmail).isEqualTo(contact.primaryEmail)
        assertThat(entity.emailStatus).isEqualTo(contact.emailStatus.name)
        assertThat(entity.locale).isEqualTo("es-AR")
        val channels: Map<String, Boolean> = objectMapper.readValue(entity.channelsJson)
        assertThat(channels)
            .containsEntry("email", true)
            .containsEntry("push", false)
        assertThat(entity.version).isEqualTo(3)
        assertThat(entity.createdAt).isEqualTo(contact.createdAt)
        assertThat(entity.updatedAt).isEqualTo(contact.updatedAt)
        assertThat(entity.quietHoursJson).isNotNull
    }

    @Test
    fun `toDomain reconstructs value objects`() {
        val entity =
            UserContactEntity().apply {
                userId = UUID.randomUUID()
                primaryEmail = "user@example.com"
                emailStatus = EmailStatus.UNVERIFIED.name
                locale = "en-US"
                channelsJson = """{"email":true,"sms":false}"""
                quietHoursJson = """{"start":"22:00","end":"06:00","timezone":"UTC"}"""
                version = 1
                createdAt = Instant.parse("2024-05-01T00:00:00Z")
                updatedAt = Instant.parse("2024-05-02T00:00:00Z")
            }

        val domain = mapper.toDomain(entity)

        assertThat(domain.userId).isEqualTo(entity.userId)
        assertThat(domain.emailStatus).isEqualTo(EmailStatus.UNVERIFIED)
        assertThat(domain.channels).containsEntry("email", true).containsEntry("sms", false)
        assertThat(domain.locale).isEqualTo(Locale("en", "US"))
        assertThat(domain.quietHours)
            .usingRecursiveComparison()
            .isEqualTo(
                QuietHours(
                    start = LocalTime.of(22, 0),
                    end = LocalTime.of(6, 0),
                    timezone = ZoneId.of("UTC"),
                ),
            )
        assertThat(domain.version).isEqualTo(1)
    }
}
