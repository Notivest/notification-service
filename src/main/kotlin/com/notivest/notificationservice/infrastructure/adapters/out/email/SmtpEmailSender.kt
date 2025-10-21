package com.notivest.notificationservice.infrastructure.adapters.out.email

import com.notivest.notificationservice.domain.email.port.EmailSender
import com.notivest.notificationservice.domain.email.port.OutboundEmail
import jakarta.mail.internet.MimeMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
@ConditionalOnBean(JavaMailSender::class)
class SmtpEmailSender(
    private val mailSender: JavaMailSender,
    @Value("\${notification.email.from}")
    private val defaultFrom: String,
) : EmailSender {

    override fun send(email: OutboundEmail) {
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name())
        helper.setFrom(email.from.ifBlank { defaultFrom })
        helper.setTo(email.to)
        helper.setSubject(email.subject)
        helper.setText(email.body, true)
        mailSender.send(message)
    }
}
