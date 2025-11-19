package com.notivest.notificationservice.infrastructure.adapters.`in`.web.notification

import com.notivest.notificationservice.application.notification.NotifyAlertUseCase
import com.notivest.notificationservice.application.notification.NotifyRecommendationUseCase
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.notification.dto.NotificationResponse
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.notification.dto.NotifyAlertRequest
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.notification.dto.NotifyRecommendationRequest
import com.notivest.notificationservice.infrastructure.mapper.NotificationMapper
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notify")
class NotificationController(
    private val notifyAlertUseCase: NotifyAlertUseCase,
    private val notifyRecommendationUseCase: NotifyRecommendationUseCase,
    private val mapper: NotificationMapper,
) {

    @PostMapping("/alert")
    fun notifyAlert(
        @Valid @RequestBody request: NotifyAlertRequest,
    ): ResponseEntity<NotificationResponse> {
        val outcome = notifyAlertUseCase.notify(mapper.toCommand(request))
        return ResponseEntity.ok(mapper.toResponse(outcome))
    }

    @PostMapping("/recommendation")
    fun notifyRecommendation(
        @Valid @RequestBody request: NotifyRecommendationRequest,
    ): ResponseEntity<NotificationResponse> {
        val outcome = notifyRecommendationUseCase.notify(mapper.toCommand(request))
        return ResponseEntity.ok(mapper.toResponse(outcome))
    }
}
