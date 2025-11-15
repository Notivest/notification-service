package com.notivest.notificationservice.infrastructure.adapters.out.email

import com.notivest.notificationservice.domain.email.port.EmailSender
import com.notivest.notificationservice.domain.email.port.OutboundEmail
import org.slf4j.LoggerFactory
import jakarta.mail.internet.MimeMessage
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
class SmtpEmailSender(
    private val mailSenderProvider: ObjectProvider<JavaMailSender>,
    @Value("\${notification.email.from}")
    private val defaultFrom: String,
) : EmailSender {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun send(email: OutboundEmail) {
        val mailSender = mailSenderProvider.ifAvailable
        if (mailSender == null) {
            logger.warn("Skipping email to {}: no JavaMailSender bean configured", email.to)
            return
        }
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name())
        helper.setFrom(email.from.ifBlank { defaultFrom })
        helper.setTo(email.to)
        helper.setSubject(email.subject)
        helper.setText(email.body, true)
        mailSender.send(message)
        logger.info("Email sent to {}", email.to)
    }
}
