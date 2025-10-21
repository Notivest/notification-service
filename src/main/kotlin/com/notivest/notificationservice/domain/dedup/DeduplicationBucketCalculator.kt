package com.notivest.notificationservice.domain.dedup

import java.time.Clock
import java.time.Duration
import java.time.Instant

class DeduplicationBucketCalculator(
    private val window: Duration,
) {
    init {
        require(!window.isZero && !window.isNegative) {
            "Deduplication window must be greater than zero"
        }
    }

    fun bucketFor(instant: Instant): Instant {
        val windowSeconds = window.seconds
        require(windowSeconds > 0) { "Deduplication window must be at least one second" }

        val adjustment = Math.floorMod(instant.epochSecond, windowSeconds)
        return Instant.ofEpochSecond(instant.epochSecond - adjustment)
    }

    fun currentBucket(clock: Clock): Instant = bucketFor(clock.instant())
}
