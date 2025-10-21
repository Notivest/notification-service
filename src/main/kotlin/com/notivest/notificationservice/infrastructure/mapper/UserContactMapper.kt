package com.notivest.notificationservice.infrastructure.mapper

import com.notivest.notificationservice.application.contact.UpsertUserContactCommand
import com.notivest.notificationservice.domain.contact.QuietHours
import com.notivest.notificationservice.domain.contact.UserContact
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.contact.dto.QuietHoursDto
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.contact.dto.UpsertUserContactRequest
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.contact.dto.UserContactResponse
import org.springframework.stereotype.Component
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Component
class UserContactMapper {
    fun toCommand(request: UpsertUserContactRequest, userId: UUID): UpsertUserContactCommand =
        UpsertUserContactCommand(
            userId = userId,
            primaryEmail = request.primaryEmail,
            emailStatus = request.emailStatus,
            locale = request.locale?.takeIf { it.isNotBlank() }?.let(Locale::forLanguageTag),
            channels = request.channels,
            quietHours = request.quietHours?.let(::toQuietHours),
        )

    fun toResponse(contact: UserContact): UserContactResponse =
        UserContactResponse(
            userId = contact.userId,
            primaryEmail = contact.primaryEmail,
            emailStatus = contact.emailStatus,
            locale = contact.locale?.toLanguageTag(),
            channels = contact.channels,
            quietHours = contact.quietHours?.let(::toQuietHoursDto),
            version = contact.version,
            updatedAt = contact.updatedAt,
            createdAt = contact.createdAt,
        )

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private fun toQuietHours(dto: QuietHoursDto): QuietHours =
        QuietHours(
            start = LocalTime.parse(dto.start, timeFormatter),
            end = LocalTime.parse(dto.end, timeFormatter),
            timezone = dto.timezone?.takeIf { it.isNotBlank() }?.let(ZoneId::of),
        )

    private fun toQuietHoursDto(source: QuietHours): QuietHoursDto =
        QuietHoursDto(
            start = source.start.format(timeFormatter),
            end = source.end.format(timeFormatter),
            timezone = source.timezone?.id,
        )
}
