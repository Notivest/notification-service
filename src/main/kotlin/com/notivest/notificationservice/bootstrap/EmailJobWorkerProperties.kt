package com.notivest.notificationservice.bootstrap

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notification.email.worker")
data class EmailJobWorkerProperties(
    val enabled: Boolean = true,
    val batchSize: Int = 25,
    val fixedDelayMs: Long = 5000,
)
