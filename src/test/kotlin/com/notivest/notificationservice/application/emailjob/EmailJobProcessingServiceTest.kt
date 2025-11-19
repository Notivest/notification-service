package com.notivest.notificationservice.application.emailjob

import com.fasterxml.jackson.databind.ObjectMapper
import com.notivest.notificationservice.bootstrap.EmailJobWorkerProperties
import com.notivest.notificationservice.domain.contact.EmailStatus
import com.notivest.notificationservice.domain.contact.UserContact
import com.notivest.notificationservice.domain.contact.port.UserContactRepository
import com.notivest.notificationservice.domain.email.EmailTemplateRenderer
import com.notivest.notificationservice.domain.email.RenderedEmailTemplate
import com.notivest.notificationservice.domain.email.port.EmailSender
import com.notivest.notificationservice.domain.email.port.OutboundEmail
import com.notivest.notificationservice.domain.emailjob.EmailJob
import com.notivest.notificationservice.domain.emailjob.EmailJobStatus
import com.notivest.notificationservice.domain.emailjob.port.EmailJobRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID

class EmailJobProcessingServiceTest {

    private val emailJobRepository: EmailJobRepository = mockk()
    private val userContactRepository: UserContactRepository = mockk()
    private val emailTemplateRenderer: EmailTemplateRenderer = mockk()
    private val emailSender: EmailSender = mockk()
    private val workerProperties = EmailJobWorkerProperties(enabled = true, batchSize = 5, fixedDelayMs = 5000)
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private lateinit var clock: Clock
    private lateinit var service: EmailJobProcessingService

    private val now: Instant = Instant.parse("2024-06-02T10:15:30Z")

    @BeforeEach
    fun setUp() {
        clearMocks(emailJobRepository, userContactRepository, emailTemplateRenderer, emailSender)
        clock = Clock.fixed(now, ZoneOffset.UTC)
        service =
            EmailJobProcessingService(
                emailJobRepository = emailJobRepository,
                userContactRepository = userContactRepository,
                emailTemplateRenderer = emailTemplateRenderer,
                emailSender = emailSender,
                clock = clock,
                workerProperties = workerProperties,
            )
    }

    @Test
    fun `returns empty result when there are no due jobs`() {
        every { emailJobRepository.findDue(now, workerProperties.batchSize) } returns emptyList()

        val result = service.processDueJobs()

        assertThat(result).isEqualTo(EmailJobProcessingResult.empty)
        verify(exactly = 1) { emailJobRepository.findDue(now, workerProperties.batchSize) }
        verify(exactly = 0) { emailSender.send(any()) }
    }

    @Test
    fun `marks job as sent when delivery succeeds`() {
        val job = pendingJob()
        val contact = verifiedContact(job.userId)
        val rendered = RenderedEmailTemplate(subject = "Subject", body = "<p>Body</p>")
        val savedJob = slot<EmailJob>()

        every { emailJobRepository.findDue(now, workerProperties.batchSize) } returns listOf(job)
        every { userContactRepository.findByUserId(job.userId) } returns contact
        every { emailTemplateRenderer.render(job.templateKey, contact.locale, job.templateData) } returns rendered
        every { emailSender.send(any<OutboundEmail>()) } just runs
        every { emailJobRepository.save(capture(savedJob)) } answers { savedJob.captured }

        val result = service.processDueJobs()

        assertThat(result.total).isEqualTo(1)
        assertThat(result.sent).isEqualTo(1)
        assertThat(result.failed).isEqualTo(0)
        assertThat(savedJob.captured.status).isEqualTo(EmailJobStatus.SENT)
        assertThat(savedJob.captured.attempts).isEqualTo(1)
        assertThat(savedJob.captured.error).isNull()
        assertThat(savedJob.captured.updatedAt).isEqualTo(now)
    }

    @Test
    fun `marks job as failed when contact is missing`() {
        val job = pendingJob()
        val savedJob = slot<EmailJob>()

        every { emailJobRepository.findDue(now, workerProperties.batchSize) } returns listOf(job)
        every { userContactRepository.findByUserId(job.userId) } returns null
        every { emailJobRepository.save(capture(savedJob)) } answers { savedJob.captured }

        val result = service.processDueJobs()

        assertThat(result.total).isEqualTo(1)
        assertThat(result.sent).isEqualTo(0)
        assertThat(result.failed).isEqualTo(1)
        assertThat(savedJob.captured.status).isEqualTo(EmailJobStatus.FAILED)
        assertThat(savedJob.captured.attempts).isEqualTo(1)
        assertThat(savedJob.captured.error).contains("Contact not found")
        verify(exactly = 0) { emailSender.send(any()) }
    }

    @Test
    fun `marks job as failed when sending throws`() {
        val job = pendingJob()
        val contact = verifiedContact(job.userId)
        val rendered = RenderedEmailTemplate(subject = "Subject", body = "<p>Body</p>")
        val savedJob = slot<EmailJob>()

        every { emailJobRepository.findDue(now, workerProperties.batchSize) } returns listOf(job)
        every { userContactRepository.findByUserId(job.userId) } returns contact
        every { emailTemplateRenderer.render(job.templateKey, contact.locale, job.templateData) } returns rendered
        every { emailSender.send(any<OutboundEmail>()) } throws IllegalStateException("SMTP unavailable")
        every { emailJobRepository.save(capture(savedJob)) } answers { savedJob.captured }

        val result = service.processDueJobs()

        assertThat(result.total).isEqualTo(1)
        assertThat(result.sent).isEqualTo(0)
        assertThat(result.failed).isEqualTo(1)
        assertThat(savedJob.captured.status).isEqualTo(EmailJobStatus.FAILED)
        assertThat(savedJob.captured.attempts).isEqualTo(1)
        assertThat(savedJob.captured.error).contains("SMTP unavailable")
    }

    private fun pendingJob(): EmailJob =
        EmailJob.pending(
            userId = UUID.randomUUID(),
            templateKey = "alert.v1",
            templateData = objectMapper.createObjectNode().put("symbol", "AAPL"),
            scheduledAt = now.minusSeconds(60),
            createdAt = now.minusSeconds(120),
        )

    private fun verifiedContact(userId: UUID): UserContact =
        UserContact(
            userId = userId,
            primaryEmail = "user@example.com",
            emailStatus = EmailStatus.VERIFIED,
            locale = Locale.US,
            channels = mapOf("email" to true),
            quietHours = null,
            version = 0,
            createdAt = now.minusSeconds(1_000),
            updatedAt = now.minusSeconds(500),
        )
}
