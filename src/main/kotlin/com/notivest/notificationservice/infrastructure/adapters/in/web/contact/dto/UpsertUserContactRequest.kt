package com.notivest.notificationservice.infrastructure.adapters.`in`.web.contact.dto

import com.notivest.notificationservice.domain.contact.EmailStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpsertUserContactRequest(
    @field:NotBlank
    @field:Email
    val primaryEmail: String,

    val emailStatus: EmailStatus,

    @field:Size(max = 10)
    val locale: String?,

    @field:NotEmpty
    val channels: Map<String, Boolean>,

    @field:Valid
    val quietHours: QuietHoursDto?,
)

data class QuietHoursDto(
    @field:NotBlank
    @field:Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d\$")
    val start: String,

    @field:NotBlank
    @field:Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d\$")
    val end: String,

    @field:Size(max = 40)
    val timezone: String?,
)
