package com.notivest.notificationservice.domain.notification

import com.notivest.notificationservice.domain.contact.QuietHours
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class QuietHoursSchedulerTest {

    private val scheduler = QuietHoursScheduler()

    @Test
    fun `returns now when quiet hours not configured`() {
        val now = Instant.parse("2024-06-01T10:00:00Z")

        val scheduled = scheduler.schedule(now, null, overrideQuietHours = false)

        assertThat(scheduled).isEqualTo(now)
    }

    @Test
    fun `returns now when override is true`() {
        val quietHours = QuietHours(LocalTime.of(22, 0), LocalTime.of(6, 0), ZoneOffset.UTC)
        val now = Instant.parse("2024-06-01T23:00:00Z")

        val scheduled = scheduler.schedule(now, quietHours, overrideQuietHours = true)

        assertThat(scheduled).isEqualTo(now)
    }

    @Test
    fun `returns now when outside quiet hours`() {
        val quietHours = QuietHours(LocalTime.of(8, 0), LocalTime.of(9, 0), ZoneOffset.UTC)
        val now = Instant.parse("2024-06-01T07:30:00Z")

        val scheduled = scheduler.schedule(now, quietHours, overrideQuietHours = false)

        assertThat(scheduled).isEqualTo(now)
    }

    @Test
    fun `defers to same day end time when within quiet hours window`() {
        val quietHours = QuietHours(LocalTime.of(8, 0), LocalTime.of(10, 0), ZoneOffset.UTC)
        val now = Instant.parse("2024-06-01T08:30:00Z")

        val scheduled = scheduler.schedule(now, quietHours, overrideQuietHours = false)

        assertThat(scheduled).isEqualTo(Instant.parse("2024-06-01T10:00:00Z"))
    }

    @Test
    fun `defers to next morning when quiet hours crosses midnight and time is late night`() {
        val quietHours = QuietHours(LocalTime.of(22, 0), LocalTime.of(6, 0), ZoneId.of("America/New_York"))
        val now = Instant.parse("2024-06-01T03:30:00Z")

        val scheduled = scheduler.schedule(now, quietHours, overrideQuietHours = false)

        assertThat(scheduled).isEqualTo(Instant.parse("2024-06-01T10:00:00Z"))
    }

    @Test
    fun `defers to same day morning when quiet hours crosses midnight and time is early`() {
        val quietHours = QuietHours(LocalTime.of(22, 0), LocalTime.of(6, 0), ZoneId.of("America/New_York"))
        val now = Instant.parse("2024-06-01T08:30:00Z") // 04:30 local

        val scheduled = scheduler.schedule(now, quietHours, overrideQuietHours = false)

        assertThat(scheduled).isEqualTo(Instant.parse("2024-06-01T10:00:00Z"))
    }
}
