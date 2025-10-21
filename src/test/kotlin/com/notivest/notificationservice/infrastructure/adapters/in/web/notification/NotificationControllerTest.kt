package com.notivest.notificationservice.infrastructure.adapters.`in`.web.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.notivest.notificationservice.application.notification.NotificationOutcome
import com.notivest.notificationservice.application.notification.NotificationRejectReason
import com.notivest.notificationservice.application.notification.NotifyAlertUseCase
import com.notivest.notificationservice.application.notification.NotifyRecommendationUseCase
import com.notivest.notificationservice.infrastructure.mapper.NotificationMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@WebMvcTest(NotificationController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(NotificationMapper::class)
class NotificationControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    @MockkBean
    private lateinit var notifyAlertUseCase: NotifyAlertUseCase

    @MockkBean
    private lateinit var notifyRecommendationUseCase: NotifyRecommendationUseCase

    @Test
    fun `POST alert returns accepted response`() {
        val jobId = UUID.randomUUID()
        val scheduledAt = Instant.parse("2024-06-01T12:00:00Z")
        every { notifyAlertUseCase.notify(any()) } returns NotificationOutcome.accepted(jobId, scheduledAt)

        mockMvc.perform(
            post("/api/v1/notify/alert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(alertPayload()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accepted").value(true))
            .andExpect(jsonPath("$.jobId").value(jobId.toString()))
            .andExpect(jsonPath("$.scheduledAt").value(scheduledAt.toString()))
    }

    @Test
    fun `POST alert returns rejection when deduplicated`() {
        every { notifyAlertUseCase.notify(any()) } returns NotificationOutcome.rejected(NotificationRejectReason.DEDUPLICATED)

        mockMvc.perform(
            post("/api/v1/notify/alert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(alertPayload()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accepted").value(false))
            .andExpect(jsonPath("$.reason").value("deduplicated"))
    }

    @Test
    fun `POST recommendation returns accepted response`() {
        val jobId = UUID.randomUUID()
        val scheduledAt = Instant.parse("2024-06-01T14:00:00Z")
        every { notifyRecommendationUseCase.notify(any()) } returns NotificationOutcome.accepted(jobId, scheduledAt)

        mockMvc.perform(
            post("/api/v1/notify/recommendation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(recommendationPayload()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accepted").value(true))
            .andExpect(jsonPath("$.jobId").value(jobId.toString()))
    }

    @Test
    fun `POST alert validates missing userId`() {
        val payload =
            objectMapper.createObjectNode().apply {
                put("fingerprint", "fp")
                put("occurredAt", Instant.parse("2024-06-01T12:00:00Z").toString())
                put("severity", "WARN")
                put("templateKey", "template.alert")
                set<ObjectNode>("templateData", objectMapper.createObjectNode())
            }

        mockMvc.perform(
            post("/api/v1/notify/alert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        )
            .andExpect(status().isBadRequest)
    }

    private fun alertPayload(): String =
        objectMapper.writeValueAsString(
            mapOf(
                "userId" to UUID.randomUUID().toString(),
                "fingerprint" to "alert-fingerprint",
                "occurredAt" to Instant.parse("2024-06-01T11:00:00Z").toString(),
                "severity" to "WARN",
                "templateKey" to "template.alert",
                "templateData" to objectMapper.createObjectNode().put("symbol", "AAPL"),
            ),
        )

    private fun recommendationPayload(): String =
        objectMapper.writeValueAsString(
            mapOf(
                "userId" to UUID.randomUUID().toString(),
                "fingerprint" to "reco-fingerprint",
                "occurredAt" to Instant.parse("2024-06-01T11:00:00Z").toString(),
                "kind" to "PORTFOLIO",
                "templateKey" to "template.reco",
                "templateData" to objectMapper.createObjectNode().put("symbol", "AAPL"),
            ),
        )
}
