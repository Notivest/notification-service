package com.notivest.notificationservice.infrastructure.adapters.out.email

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.notivest.notificationservice.domain.email.EmailTemplateRenderer
import com.notivest.notificationservice.domain.email.RenderedEmailTemplate
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.context.Context
import java.util.Locale

@Component
class ThymeleafEmailTemplateRenderer(
    private val templateEngine: SpringTemplateEngine,
    private val messageSource: MessageSource,
    private val objectMapper: ObjectMapper,
) : EmailTemplateRenderer {

    override fun render(templateKey: String, locale: Locale?, data: JsonNode): RenderedEmailTemplate {
        val templateName = resolveTemplateName(templateKey)
        val variables = toVariables(data)
        val messageLocale = locale ?: Locale.ENGLISH
        val context = Context(messageLocale).apply {
            setVariable("data", variables)
        }

        val recipientName = resolveRecipientName(variables, messageLocale)
        val subjectTarget = resolveSubjectTarget(variables)

        context.setVariable("recipientName", recipientName)
        context.setVariable("subjectTarget", subjectTarget)

        val subject = resolveSubject(templateName, messageLocale, subjectTarget, variables)
        val body = templateEngine.process("email/$templateName", context)

        return RenderedEmailTemplate(subject = subject, body = body)
    }

    private fun resolveTemplateName(templateKey: String): String =
        templateKey.substringBefore('.')

    private fun toVariables(data: JsonNode): Map<String, Any?> =
        if (data.isMissingNode || data.isNull) {
            emptyMap()
        } else {
            objectMapper.convertValue(data, object : TypeReference<Map<String, Any?>>() {})
        }

    private fun resolveSubject(templateName: String, locale: Locale, subjectTarget: String, variables: Map<String, Any?>): String {
        val messageKey = "email.$templateName.subject"
        val args = subjectArguments(templateName, subjectTarget, variables)
        return messageSource.getMessage(messageKey, args, messageKey, locale) ?: messageKey
    }

    private fun subjectArguments(templateName: String, subjectTarget: String, variables: Map<String, Any?>): Array<Any> =
        when (templateName) {
            "alert" -> arrayOf(subjectTarget)
            "recommendation" -> arrayOf(subjectTarget)
            else -> arrayOf((variables["title"] ?: subjectTarget).toString())
        }

    private fun resolveRecipientName(variables: Map<String, Any?>, locale: Locale): String {
        val candidate = (variables["recipientName"] as? String)?.takeIf { it.isNotBlank() }
        return candidate ?: messageSource.getMessage("email.common.recipientFallback", null, "there", locale) ?: "there"
    }

    private fun resolveSubjectTarget(variables: Map<String, Any?>): String =
        listOfNotNull(
            variables["symbol"],
            variables["title"],
        ).firstOrNull()?.toString() ?: "item"
}
