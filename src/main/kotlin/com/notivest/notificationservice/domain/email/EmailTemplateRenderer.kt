package com.notivest.notificationservice.domain.email

import com.fasterxml.jackson.databind.JsonNode
import java.util.Locale

interface EmailTemplateRenderer {
    fun render(templateKey: String, locale: Locale?, data: JsonNode): RenderedEmailTemplate
}

data class RenderedEmailTemplate(
    val subject: String,
    val body: String,
)
