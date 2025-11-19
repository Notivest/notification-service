package com.notivest.notificationservice.application.notification

interface NotifyAlertUseCase {
    fun notify(command: NotifyAlertCommand): NotificationOutcome
}
