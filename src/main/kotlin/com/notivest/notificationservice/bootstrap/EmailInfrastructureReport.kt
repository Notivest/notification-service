package com.notivest.notificationservice.bootstrap

import com.notivest.notificationservice.domain.email.port.EmailSender
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class EmailInfrastructureReport(
    private val applicationContext: ApplicationContext,
) : ApplicationListener<ApplicationReadyEvent> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        val beanNames = applicationContext.getBeanNamesForType(JavaMailSender::class.java)
        val emailSenderBeans = applicationContext.getBeanNamesForType(EmailSender::class.java)
        val javaMailSenderClass =
            beanNames.firstOrNull()?.let { applicationContext.getBean(it).javaClass.name } ?: "none"
        logger.info(
            "JavaMailSender beans detected: {} (class={}); EmailSender beans: {}",
            beanNames.toList(),
            javaMailSenderClass,
            emailSenderBeans.toList(),
        )
    }
}
