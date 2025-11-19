package com.notivest.notificationservice.domain.contact.port

import com.notivest.notificationservice.domain.contact.UserContact
import java.util.UUID

interface UserContactRepository {
    fun findByUserId(userId: UUID): UserContact?
    fun save(contact: UserContact): UserContact
}
