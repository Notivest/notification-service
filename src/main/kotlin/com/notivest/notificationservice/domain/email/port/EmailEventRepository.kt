package com.notivest.notificationservice.domain.email.port

import com.notivest.notificationservice.domain.email.EmailEvent

interface EmailEventRepository {
    fun save(event: EmailEvent): EmailEvent
}
