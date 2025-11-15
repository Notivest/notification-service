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
        assertThat(rendered.body).contains("Hello Gonza,")
        assertThat(rendered.body).contains("We detected a new alert for AAPL.")
        assertThat(rendered.body).contains("We spotted unusual activity affecting AAPL. Here is the snapshot so you can respond confidently.")
        assertThat(rendered.body).contains("<span>WARN</span>")
        assertThat(rendered.body).contains("You received this alert based on your current notification preferences.")
        assertThat(rendered.body).doesNotContain("th:text")
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
        assertThat(rendered.body).contains("Hola Gonza,")
        assertThat(rendered.body).contains("Detectamos una nueva alerta para AAPL.")
        assertThat(rendered.body).contains("Registramos actividad inusual en AAPL. A continuación, un resumen para que puedas actuar con rapidez.")
        assertThat(rendered.body).contains("<span>CRITICAL</span>")
        assertThat(rendered.body).contains("Recibiste este aviso según tus preferencias de notificación vigentes.")
        assertThat(rendered.body).doesNotContain("th:text")
    }

    @Test
    fun `render alert template includes holdings section`() {
        val data = objectMapper.readTree(
            """
            {
              "recipientName": "Gonza",
              "symbol": "AAPL",
              "holdings": [
                {
                  "portfolioName": "Cartera USD",
                  "quantity": 12.5,
                  "avgCost": 188.12,
                  "updatedAt": "2024-06-01T10:15:00Z"
                }
              ]
            }
            """.trimIndent(),
        )

        val rendered = renderer.render("alert.v1", Locale.ENGLISH, data)

        assertThat(rendered.body).contains("Your holdings in this asset")
        assertThat(rendered.body).contains("Cartera USD")
        assertThat(rendered.body).contains("188.12")
    }

    @Test
    fun `render alert template shows empty message when no holdings`() {
        val data = objectMapper.readTree(
            """
            {
              "recipientName": "Gonza",
              "symbol": "AAPL",
              "holdings": []
            }
            """.trimIndent(),
        )

        val rendered = renderer.render("alert.v1", Locale.ENGLISH, data)

        assertThat(rendered.body).contains("You currently have no holdings for AAPL.")
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
        assertThat(rendered.body).contains("Hello Gonza,")
        assertThat(rendered.body).contains("Here is a new recommendation for SP500.")
        assertThat(rendered.body).contains("Rebalance portfolio")
        assertThat(rendered.body).contains("Consider hedging")
        assertThat(rendered.body).contains("Recommendation type")
        assertThat(rendered.body).contains("Review your dashboard to take action.")
        assertThat(rendered.body).doesNotContain("th:text")
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
        assertThat(rendered.body).contains("Hola Gonza,")
        assertThat(rendered.body).contains("Tenemos una nueva recomendación para SP500.")
        assertThat(rendered.body).contains("Rebalancear cartera")
        assertThat(rendered.body).contains("Considerar cobertura")
        assertThat(rendered.body).contains("Tipo de recomendación")
        assertThat(rendered.body).contains("Consulta tu panel para tomar acción.")
        assertThat(rendered.body).doesNotContain("th:text")
    }

    @Test
    fun `fallback to base alert template when variant suffix missing`() {
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

        val rendered = renderer.render("alert-default", Locale.ENGLISH, data)

        assertThat(rendered.subject).isEqualTo("Alert: AAPL")
        assertThat(rendered.body).contains("We detected a new alert for AAPL.")
        assertThat(rendered.body).contains("<span>WARN</span>")
        assertThat(rendered.body).doesNotContain("th:text")
    }

}
