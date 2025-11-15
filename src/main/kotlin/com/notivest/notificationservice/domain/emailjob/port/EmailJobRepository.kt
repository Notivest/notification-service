package com.notivest.notificationservice.domain.emailjob.port

import com.notivest.notificationservice.domain.emailjob.EmailJob
import java.time.Instant

interface EmailJobRepository {
    fun save(job: EmailJob): EmailJob
    fun findDue(now: Instant, limit: Int): List<EmailJob>
}
