package com.notivest.notificationservice.bootstrap

import com.notivest.notificationservice.domain.notification.QuietHoursScheduler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NotificationConfig {

    @Bean
    fun quietHoursScheduler(): QuietHoursScheduler = QuietHoursScheduler()
}
