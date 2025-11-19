package com.notivest.notificationservice.application.webhook

interface RegisterEmailEventUseCase {
    fun register(command: RegisterEmailEventCommand)
}
