package com.notivest.notificationservice.infrastructure.mapper

import com.notivest.notificationservice.application.contact.UpsertUserContactCommand
import com.notivest.notificationservice.domain.contact.EmailStatus
import com.notivest.notificationservice.domain.contact.QuietHours
import com.notivest.notificationservice.domain.contact.UserContact
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.contact.dto.QuietHoursDto
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.contact.dto.UpsertUserContactRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

class UserContactMapperTest {

    private val mapper = UserContactMapper()

    @Test
    fun `toCommand maps locale and quiet hours`() {
        val dto =
            UpsertUserContactRequest(
                primaryEmail = "user@example.com",
                emailStatus = EmailStatus.UNVERIFIED,
                locale = " ",
                channels = mapOf("email" to true),
                quietHours =
                    QuietHoursDto(
                        start = "21:00",
                        end = "06:30",
                        timezone = "",
                    ),
            )

        val userId = UUID.randomUUID()
        val command: UpsertUserContactCommand = mapper.toCommand(dto, userId)

        assertThat(command.userId).isEqualTo(userId)
        assertThat(command.locale).isNull()
        assertThat(command.quietHours)
            .usingRecursiveComparison()
            .isEqualTo(
                QuietHours(
                    start = LocalTime.of(21, 0),
                    end = LocalTime.of(6, 30),
                    timezone = null,
                ),
            )
    }

    @Test
    fun `toResponse renders expected payload`() {
        val contact =
            UserContact(
                userId = UUID.randomUUID(),
                primaryEmail = "user@example.com",
                emailStatus = EmailStatus.VERIFIED,
                locale = Locale("es", "AR"),
                channels = mapOf("email" to true, "sms" to false),
                quietHours =
                    QuietHours(
                        start = LocalTime.of(22, 0),
                        end = LocalTime.of(7, 0),
                        timezone = ZoneId.of("America/Buenos_Aires"),
                    ),
                version = 5,
                createdAt = Instant.parse("2024-05-01T12:00:00Z"),
                updatedAt = Instant.parse("2024-05-02T12:00:00Z"),
            )

        val response = mapper.toResponse(contact)

        assertThat(response.userId).isEqualTo(contact.userId)
        assertThat(response.locale).isEqualTo("es-AR")
        assertThat(response.version).isEqualTo(5)
        assertThat(response.quietHours)
            .usingRecursiveComparison()
            .isEqualTo(
                QuietHoursDto(
                    start = "22:00",
                    end = "07:00",
                    timezone = "America/Buenos_Aires",
                ),
            )
    }
}
