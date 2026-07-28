package com.notivest.notificationservice.infrastructure.adapters.out.email

import com.notivest.notificationservice.domain.email.port.EmailSender
import com.notivest.notificationservice.domain.email.port.OutboundEmail
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
@ConditionalOnProperty(name = ["notification.email.provider"], havingValue = "sendgrid-api")
class SendGridApiEmailSender(
    restClientBuilder: RestClient.Builder,
    @Value("\${notification.email.sendgrid.api-key:}") private val apiKey: String,
    @Value("\${notification.email.sendgrid.api-url:https://api.sendgrid.com/v3/mail/send}") apiUrl: String,
    @Value("\${notification.email.from}") private val defaultFrom: String,
) : EmailSender {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val restClient = restClientBuilder.baseUrl(apiUrl).build()

    override fun send(email: OutboundEmail) {
        check(apiKey.isNotBlank()) { "SENDGRID_API_KEY must be configured when using the SendGrid API email provider" }

        restClient.post()
            .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                mapOf(
                    "personalizations" to listOf(mapOf("to" to listOf(mapOf("email" to email.to)))),
                    "from" to mapOf("email" to email.from.ifBlank { defaultFrom }),
                    "subject" to email.subject,
                    "content" to listOf(mapOf("type" to "text/html", "value" to email.body)),
                ),
            )
            .retrieve()
            .toBodilessEntity()

        logger.info("Email accepted by SendGrid")
    }
}
