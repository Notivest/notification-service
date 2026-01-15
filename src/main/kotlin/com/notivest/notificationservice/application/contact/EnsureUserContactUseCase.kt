package com.notivest.notificationservice.application.contact

import com.notivest.notificationservice.domain.contact.UserContact

fun interface EnsureUserContactUseCase {
    fun ensure(command: UpsertUserContactCommand): UserContact
}
