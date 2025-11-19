package com.notivest.notificationservice.bootstrap

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockConfig {
    @Bean
    fun utcClock(): Clock = Clock.systemUTC()
}
