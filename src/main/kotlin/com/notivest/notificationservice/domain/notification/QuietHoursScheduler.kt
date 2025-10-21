package com.notivest.notificationservice.domain.notification

import com.notivest.notificationservice.domain.contact.QuietHours
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class QuietHoursScheduler {

    fun schedule(
        now: Instant,
        quietHours: QuietHours?,
        overrideQuietHours: Boolean,
    ): Instant =
        if (overrideQuietHours || quietHours == null) {
            now
        } else {
            nextAllowedInstant(now, quietHours)
        }

    private fun nextAllowedInstant(now: Instant, quietHours: QuietHours): Instant {
        val zone: ZoneId = quietHours.timezone ?: ZoneOffset.UTC
        val zonedNow = now.atZone(zone)
        val start = quietHours.start
        val end = quietHours.end

        if (start == end) {
            return now
        }

        val inQuiet = isWithinQuietWindow(zonedNow.toLocalTime(), start, end)
        if (!inQuiet) {
            return now
        }

        val scheduledLocalDateTime =
            when {
                start <= end -> zonedNow.toLocalDate().atTime(end)
                zonedNow.toLocalTime() >= start -> zonedNow.toLocalDate().plusDays(1).atTime(end)
                else -> zonedNow.toLocalDate().atTime(end)
            }

        val scheduledZoned = scheduledLocalDateTime.atZone(zone)
        val ensuredFuture = ensureFutureInstant(scheduledZoned.toInstant(), now, zone, quietHours.end)
        return ensuredFuture
    }

    private fun ensureFutureInstant(
        candidate: Instant,
        now: Instant,
        zone: ZoneId,
        end: LocalTime,
    ): Instant =
        if (!candidate.isAfter(now)) {
            LocalDate.ofInstant(now, zone)
                .plusDays(1)
                .atTime(end)
                .atZone(zone)
                .toInstant()
        } else {
            candidate
        }

    private fun isWithinQuietWindow(
        current: LocalTime,
        start: LocalTime,
        end: LocalTime,
    ): Boolean =
        if (start <= end) {
            !current.isBefore(start) && current.isBefore(end)
        } else {
            !current.isBefore(start) || current.isBefore(end)
        }
}
