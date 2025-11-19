package com.notivest.notificationservice.infrastructure.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class WebhookAuthenticationService(
    @Value("\${notification.webhooks.email.token:}")
    private val token: String,
    @Value("\${notification.webhooks.email.allowed-ips:}")
    private val allowedIpsProperty: String,
) {

    private val allowedIps: Set<String> =
        allowedIpsProperty
            .split(',')
            .map(String::trim)
            .filter { it.isNotEmpty() }
            .toSet()

    fun isAuthorized(request: HttpServletRequest): Boolean {
        val headerToken = request.getHeader(WEBHOOK_TOKEN_HEADER)
        if (token.isNotBlank()) {
            if (token == headerToken) {
                return true
            }
        }

        if (allowedIps.isNotEmpty()) {
            val remoteIp = request.remoteAddr
            if (allowedIps.contains(remoteIp)) {
                return true
            }
        }

        return false
    }

    companion object {
        const val WEBHOOK_TOKEN_HEADER = "X-Webhook-Token"
    }
}
