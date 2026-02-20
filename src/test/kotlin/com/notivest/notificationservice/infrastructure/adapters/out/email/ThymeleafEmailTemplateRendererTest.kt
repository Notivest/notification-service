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
              "ruleTitle": "Price alert",
              "ruleNote": "Notify me when it breaks resistance",
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

        assertThat(rendered.subject).isEqualTo("Alert: Price alert")
        assertThat(rendered.body).contains("Hello Gonza,")
        assertThat(rendered.body).contains("We detected a new alert for AAPL.")
        assertThat(rendered.body).contains("We spotted unusual activity affecting AAPL. Here is the snapshot so you can respond confidently.")
        assertThat(rendered.body).contains("Price alert")
        assertThat(rendered.body).contains("Notify me when it breaks resistance")
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
              "ruleTitle": "Alerta de precio",
              "ruleNote": "Avisame cuando rompa resistencia",
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

        assertThat(rendered.subject).isEqualTo("Alerta: Alerta de precio")
        assertThat(rendered.body).contains("Hola Gonza,")
        assertThat(rendered.body).contains("Detectamos una nueva alerta para AAPL.")
        assertThat(rendered.body).contains("Registramos actividad inusual en AAPL. A continuación, un resumen para que puedas actuar con rapidez.")
        assertThat(rendered.body).contains("Alerta de precio")
        assertThat(rendered.body).contains("Avisame cuando rompa resistencia")
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
                  "bookValue": 2351.50,
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
        assertThat(rendered.body).contains("2351.5")
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
    fun `render alert template hides technical ids and raw payload keys`() {
        val data = objectMapper.readTree(
            """
            {
              "recipientName": "Gonza",
              "symbol": "AAPL",
              "eventId": "550e8400-e29b-41d4-a716-446655440000",
              "ruleId": "550e8400-e29b-41d4-a716-446655440001",
              "payload": {
                "lastPrice": "188.12",
                "delta": "8.12",
                "asOf": "2025-01-01T10:00:00Z"
              }
            }
            """.trimIndent(),
        )

        val rendered = renderer.render("alert.v1", Locale.ENGLISH, data)

        assertThat(rendered.body).doesNotContain("Event ID:")
        assertThat(rendered.body).doesNotContain("Rule ID:")
        assertThat(rendered.body).contains("Difference vs threshold")
        assertThat(rendered.body).contains("Detected at")
        assertThat(rendered.body).doesNotContain(">asOf<")
        assertThat(rendered.body).doesNotContain(">delta<")
    }

    @Test
    fun `render alert template normalizes generic payload and rule params`() {
        val data = objectMapper.readTree(
            """
            {
              "recipientName": "Gonza",
              "symbol": "AAPL",
              "ruleKind": "ATR_BREAKOUT",
              "payload": {
                "distanceATR": 2.5,
                "atrPeriod": 14,
                "direction": "CROSSING_UP"
              },
              "ruleParams": {
                "distanceATR": 2.5,
                "atrPeriod": 14,
                "basis": "HLC3"
              }
            }
            """.trimIndent(),
        )

        val rendered = renderer.render("alert.v1", Locale.ENGLISH, data)

        assertThat(rendered.body).contains("Distance (ATR)")
        assertThat(rendered.body).contains("ATR period")
        assertThat(rendered.body).contains("CROSSING UP")
        assertThat(rendered.body).contains("Basis")
        assertThat(rendered.body).contains("HLC3")
        assertThat(rendered.body).doesNotContain(">distanceATR<")
        assertThat(rendered.body).doesNotContain(">atrPeriod<")
    }

    @Test
    fun `render alert template supports every alert kind without failing`() {
        val allAlertKinds = listOf(
            "PRICE_THRESHOLD",
            "PCT_CHANGE",
            "DRAWDOWN",
            "MA_CROSS",
            "RSI",
            "VOLUME_SPIKE",
            "ATR_BREAKOUT",
            "TRAILING_STOP",
            "BB_TOUCH",
            "MACD_CROSS",
            "GAP_SESSION",
            "DRAWDOWN_FROM_MAX",
            "CANDLE_PATTERN",
            "POSITION_PNL",
            "PORTFOLIO_DRAWDOWN",
            "REBALANCE_DRIFT",
            "EARNINGS_WINDOW",
            "NEWS_SENTIMENT",
        )

        allAlertKinds.forEach { kind ->
            val data = objectMapper.readTree(
                """
                {
                  "recipientName": "Gonza",
                  "symbol": "AAPL",
                  "ruleKind": "$kind",
                  "payload": {
                    "asOf": "2025-01-01T10:00:00Z",
                    "customMetric": 42
                  },
                  "ruleParams": {
                    "operator": "GTE",
                    "value": 100
                  }
                }
                """.trimIndent(),
            )

            val rendered = renderer.render("alert.v1", Locale.ENGLISH, data)

            assertThat(rendered.body).contains("Rule type")
            assertThat(rendered.body).contains(kind.replace("_", " "))
            assertThat(rendered.body).contains("Custom Metric")
            assertThat(rendered.body).contains("Condition")
        }
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
