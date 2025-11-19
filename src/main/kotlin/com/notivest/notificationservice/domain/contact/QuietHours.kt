package com.notivest.notificationservice.domain.contact

import java.time.LocalTime
import java.time.ZoneId

data class QuietHours(
    val start: LocalTime,
    val end: LocalTime,
    val timezone: ZoneId?,
)
