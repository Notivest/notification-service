package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.usercontact

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.notivest.notificationservice.domain.contact.EmailStatus
import com.notivest.notificationservice.domain.contact.QuietHours
import com.notivest.notificationservice.domain.contact.UserContact
import org.springframework.stereotype.Component
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
class UserContactEntityMapper(
    private val objectMapper: ObjectMapper,
) {
    fun toEntity(source: UserContact): UserContactEntity =
        UserContactEntity().apply {
            userId = source.userId
            primaryEmail = source.primaryEmail
            emailStatus = source.emailStatus.name
            locale = source.locale?.toLanguageTag()
            channelsJson = objectMapper.writeValueAsString(source.channels)
            quietHoursJson = source.quietHours?.let(::quietHoursToJson)
            version = source.version
            createdAt = source.createdAt
            updatedAt = source.updatedAt
        }

    fun toDomain(entity: UserContactEntity): UserContact =
        UserContact(
            userId = entity.userId,
            primaryEmail = entity.primaryEmail,
            emailStatus = EmailStatus.valueOf(entity.emailStatus),
            locale = entity.locale?.let(Locale::forLanguageTag),
            channels = objectMapper.readValue(entity.channelsJson, CHANNELS_TYPE_REF),
            quietHours = entity.quietHoursJson?.let(::quietHoursFromJson),
            version = entity.version,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )

    private fun quietHoursToJson(quietHours: QuietHours): String =
        objectMapper.writeValueAsString(
            mapOf(
                "start" to timeFormatter.format(quietHours.start),
                "end" to timeFormatter.format(quietHours.end),
                "timezone" to quietHours.timezone?.id,
            ),
        )

    private fun quietHoursFromJson(value: String): QuietHours? =
        objectMapper.readValue(value, QuietHoursPayload::class.java).let {
            QuietHours(
                start = LocalTime.parse(it.start, timeFormatter),
                end = LocalTime.parse(it.end, timeFormatter),
                timezone = it.timezone?.let(ZoneId::of),
            )
        }

    private data class QuietHoursPayload(
        val start: String,
        val end: String,
        val timezone: String?,
    )

    companion object {
        private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val CHANNELS_TYPE_REF = object : TypeReference<Map<String, Boolean>>() {}
    }
}
