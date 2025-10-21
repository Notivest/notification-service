package com.notivest.notificationservice.infrastructure.adapters.out.email

import com.fasterxml.jackson.databind.ObjectMapper
import com.notivest.notificationservice.domain.email.EmailTemplateRenderer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.support.ResourceBundleMessageSource
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.nio.charset.StandardCharsets
import java.util.Locale

class ThymeleafEmailTemplateRendererTest {

    private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()
    private val renderer: EmailTemplateRenderer

    init {
        val templateResolver = ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = StandardCharsets.UTF_8.name()
            templateMode = TemplateMode.HTML
            isCacheable = false
        }

        val messageSource = ResourceBundleMessageSource().apply {
            setBasename("messages")
            setDefaultEncoding(StandardCharsets.UTF_8.name())
        }

        val templateEngine = SpringTemplateEngine().apply {
            setTemplateResolver(templateResolver)
            setTemplateEngineMessageSource(messageSource)
        }

        renderer = ThymeleafEmailTemplateRenderer(
            templateEngine = templateEngine,
            messageSource = messageSource,
            objectMapper = objectMapper,
        )
    }

    @Test
    fun `render alert template in English`() {
        val data = objectMapper.readTree(
            """
            {
              "recipientName": "Gonza",
              "symbol": "AAPL",
              "severity": "WARN",
              "occurredAt": "2024-06-01T10:15:00Z",
              "details": {
                "price": "188.12",
                "timeframe": "H1"
              }
            }
            """.trimIndent(),
        )

        val rendered = renderer.render("alert.v1", Locale.ENGLISH, data)

        assertThat(rendered.subject).isEqualTo("Alert: AAPL")
        assertThat(rendered.body.trim()).isEqualTo(loadSnapshot("alert_en.html").trim())
    }

    @Test
    fun `render alert template in Spanish`() {
        val data = objectMapper.readTree(
            """
            {
              "recipientName": "Gonza",
              "symbol": "AAPL",
              "severity": "CRITICAL",
              "occurredAt": "2024-06-01T10:15:00Z",
              "details": {
                "price": "188.12",
                "timeframe": "H1"
              }
            }
            """.trimIndent(),
        )

        val rendered = renderer.render("alert.v1", Locale.forLanguageTag("es-AR"), data)

        assertThat(rendered.subject).isEqualTo("Alerta: AAPL")
        assertThat(rendered.body.trim()).isEqualTo(loadSnapshot("alert_es.html").trim())
    }

    @Test
    fun `render recommendation template in English`() {
        val data = objectMapper.readTree(
            """
            {
              "recipientName": "Gonza",
              "symbol": "SP500",
              "kind": "PORTFOLIO",
              "highlights": ["Rebalance portfolio", "Consider hedging"],
              "details": {
                "score": "0.82",
                "confidence": "HIGH"
              }
            }
            """.trimIndent(),
        )

        val rendered = renderer.render("recommendation.v1", Locale.ENGLISH, data)

        assertThat(rendered.subject).isEqualTo("Recommendation: SP500")
        assertThat(rendered.body.trim()).isEqualTo(loadSnapshot("recommendation_en.html").trim())
    }

    @Test
    fun `render recommendation template in Spanish`() {
        val data = objectMapper.readTree(
            """
            {
              "recipientName": "Gonza",
              "symbol": "SP500",
              "kind": "PORTFOLIO",
              "highlights": ["Rebalancear cartera", "Considerar cobertura"],
              "details": {
                "score": "0.82",
                "confidence": "ALTA"
              }
            }
            """.trimIndent(),
        )

        val rendered = renderer.render("recommendation.v1", Locale.forLanguageTag("es-AR"), data)

        assertThat(rendered.subject).isEqualTo("Recomendación: SP500")
        assertThat(rendered.body.trim()).isEqualTo(loadSnapshot("recommendation_es.html").trim())
    }

    private fun loadSnapshot(name: String): String =
        ThymeleafEmailTemplateRendererTest::class.java.getResource("/snapshots/$name")
            ?.readText(StandardCharsets.UTF_8)
            ?: error("Snapshot $name not found")
}
