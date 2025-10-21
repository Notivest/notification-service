package com.notivest.notificationservice.infrastructure.adapters.`in`.web.webhook

import com.notivest.notificationservice.application.webhook.RegisterEmailEventUseCase
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.webhook.dto.EmailWebhookRequest
import com.notivest.notificationservice.infrastructure.security.WebhookAuthenticationService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1/webhooks/email")
class EmailWebhookController(
    private val registerEmailEventUseCase: RegisterEmailEventUseCase,
    private val mapper: EmailWebhookMapper,
    private val authenticationService: WebhookAuthenticationService,
) {

    @PostMapping
    fun handleWebhook(
        request: HttpServletRequest,
        @Valid @RequestBody payload: EmailWebhookRequest,
    ): ResponseEntity<Unit> {
        if (!authenticationService.isAuthorized(request)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized webhook request")
        }

        registerEmailEventUseCase.register(mapper.toCommand(payload))
        return ResponseEntity.accepted().build()
    }
}
