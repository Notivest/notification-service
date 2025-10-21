package com.notivest.notificationservice.domain.emailjob.port

import com.notivest.notificationservice.domain.emailjob.EmailJob

interface EmailJobRepository {
    fun save(job: EmailJob): EmailJob
}
