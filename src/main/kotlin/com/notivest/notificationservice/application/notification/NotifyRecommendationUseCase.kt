package com.notivest.notificationservice.application.notification

interface NotifyRecommendationUseCase {
    fun notify(command: NotifyRecommendationCommand): NotificationOutcome
}
