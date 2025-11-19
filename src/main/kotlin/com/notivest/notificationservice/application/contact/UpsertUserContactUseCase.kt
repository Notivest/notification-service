package com.notivest.notificationservice.application.contact

import com.notivest.notificationservice.domain.contact.UserContact

fun interface UpsertUserContactUseCase {
    fun upsert(command: UpsertUserContactCommand): UserContact
}
