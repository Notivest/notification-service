package com.notivest.notificationservice.application.contact

import com.notivest.notificationservice.domain.contact.UserContact
import java.util.UUID

fun interface GetUserContactQuery {
    fun get(userId: UUID): UserContact?
}
