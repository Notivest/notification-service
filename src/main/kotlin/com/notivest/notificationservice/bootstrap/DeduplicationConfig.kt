package com.notivest.notificationservice.bootstrap

import com.notivest.notificationservice.domain.dedup.DeduplicationBucketCalculator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class DeduplicationConfig(
    @Value("\${dedup.window-minutes:5}")
    private val windowMinutes: Long,
) {

    @Bean
    fun deduplicationBucketCalculator(): DeduplicationBucketCalculator {
        require(windowMinutes > 0) {
            "Property 'dedup.window-minutes' must be greater than zero"
        }
        return DeduplicationBucketCalculator(Duration.ofMinutes(windowMinutes))
    }
}
