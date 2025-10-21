package com.notivest.notificationservice.domain.dedup

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class DeduplicationBucketCalculatorTest {

    private val calculator = DeduplicationBucketCalculator(Duration.ofMinutes(5))

    @Test
    fun `bucketFor rounds down to window boundary`() {
        val instant = Instant.parse("2024-06-01T10:07:42Z")

        val bucket = calculator.bucketFor(instant)

        assertThat(bucket).isEqualTo(Instant.parse("2024-06-01T10:05:00Z"))
    }

    @Test
    fun `currentBucket uses provided clock`() {
        val clock = Clock.fixed(Instant.parse("2024-06-01T10:12:00Z"), ZoneOffset.UTC)

        val bucket = calculator.currentBucket(clock)

        assertThat(bucket).isEqualTo(Instant.parse("2024-06-01T10:10:00Z"))
    }
}
