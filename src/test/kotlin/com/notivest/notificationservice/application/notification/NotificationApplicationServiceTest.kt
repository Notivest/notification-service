package com.notivest.notificationservice.application.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.notivest.notificationservice.domain.contact.EmailStatus
import com.notivest.notificationservice.domain.contact.QuietHours
import com.notivest.notificationservice.domain.contact.UserContact
import com.notivest.notificationservice.domain.contact.port.UserContactRepository
import com.notivest.notificationservice.domain.dedup.DeduplicationBucketCalculator
import com.notivest.notificationservice.domain.dedup.port.DedupKeyRepository
import com.notivest.notificationservice.domain.emailjob.EmailJob
import com.notivest.notificationservice.domain.emailjob.port.EmailJobRepository
import com.notivest.notificationservice.domain.notification.QuietHoursScheduler
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID

class NotificationApplicationServiceTest {

    private val dedupKeyRepository: DedupKeyRepository = mockk()
    private val userContactRepository: UserContactRepository = mockk()
    private val emailJobRepository: EmailJobRepository = mockk()
    private val quietHoursScheduler = QuietHoursScheduler()
    private val deduplicationBucketCalculator = DeduplicationBucketCalculator(Duration.ofMinutes(5))
    private val alertTemplateDataEnricher: AlertTemplateDataEnricher = mockk()
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    private val userId = UUID.randomUUID()
    private val fingerprint = "fp-123"
    private val occurredAt = Instant.parse("2024-06-01T10:15:00Z")
    private val contact = buildContact()

    @BeforeEach
    fun setUp() {
        clearMocks(dedupKeyRepository, userContactRepository, emailJobRepository, alertTemplateDataEnricher)
        every { alertTemplateDataEnricher.enrich(any(), any()) } answers { secondArg() }
    }

    @Test
    fun `rejects when contact not found`() {
        every { userContactRepository.findByUserId(userId) } returns null

        val service = service(clockInstant = Instant.parse("2024-06-01T12:00:00Z"))
        val outcome = service.notify(alertCommand(severity = "WARN"))

        assertThat(outcome.accepted).isFalse()
        assertThat(outcome.reason).isEqualTo(NotificationRejectReason.CONTACT_NOT_FOUND)
        verify(exactly = 0) { dedupKeyRepository.insertIfAbsent(any(), any(), any()) }
        verify(exactly = 0) { emailJobRepository.save(any()) }
    }

    @Test
    fun `rejects when email channel disabled`() {
        every { userContactRepository.findByUserId(userId) } returns contact.copy(channels = mapOf("email" to false))

        val service = service(clockInstant = Instant.parse("2024-06-01T12:00:00Z"))
        val outcome = service.notify(alertCommand(severity = "WARN"))

        assertThat(outcome.accepted).isFalse()
        assertThat(outcome.reason).isEqualTo(NotificationRejectReason.EMAIL_CHANNEL_DISABLED)
        verify(exactly = 0) { dedupKeyRepository.insertIfAbsent(any(), any(), any()) }
        verify(exactly = 0) { emailJobRepository.save(any()) }
    }

    @Test
    fun `rejects when email status blocked`() {
        every { userContactRepository.findByUserId(userId) } returns contact.copy(emailStatus = EmailStatus.UNSUB)

        val service = service(clockInstant = Instant.parse("2024-06-01T12:00:00Z"))
        val outcome = service.notify(alertCommand(severity = "WARN"))

        assertThat(outcome.accepted).isFalse()
        assertThat(outcome.reason).isEqualTo(NotificationRejectReason.EMAIL_STATUS_BLOCKED)
        verify(exactly = 0) { dedupKeyRepository.insertIfAbsent(any(), any(), any()) }
        verify(exactly = 0) { emailJobRepository.save(any()) }
    }

    @Test
    fun `rejects when dedup key already exists`() {
        every { userContactRepository.findByUserId(userId) } returns contact
        every { dedupKeyRepository.insertIfAbsent(userId, fingerprint, deduplicationBucketCalculator.bucketFor(occurredAt)) } returns false

        val service = service(clockInstant = Instant.parse("2024-06-01T12:00:00Z"))
        val outcome = service.notify(alertCommand(severity = "WARN"))

        assertThat(outcome.accepted).isFalse()
        assertThat(outcome.reason).isEqualTo(NotificationRejectReason.DEDUPLICATED)
        verify(exactly = 0) { emailJobRepository.save(any()) }
    }

    @Test
    fun `enqueues email job when alert is accepted`() {
        every { userContactRepository.findByUserId(userId) } returns contact
        every { dedupKeyRepository.insertIfAbsent(userId, fingerprint, deduplicationBucketCalculator.bucketFor(occurredAt)) } returns true
        val capturedJob = slot<EmailJob>()
        every { emailJobRepository.save(capture(capturedJob)) } answers { capturedJob.captured }

        val clockInstant = Instant.parse("2024-06-01T12:00:00Z")
        val service = service(clockInstant)
        val outcome = service.notify(alertCommand(severity = "WARN"))

        assertThat(outcome.accepted).isTrue()
        assertThat(outcome.jobId).isNotNull()
        assertThat(outcome.scheduledAt).isEqualTo(clockInstant)
        assertThat(capturedJob.captured.scheduledAt).isEqualTo(clockInstant)
    }

    @Test
    fun `bypasses quiet hours for critical severity`() {
        val quietHours = QuietHours(LocalTime.of(22, 0), LocalTime.of(6, 0), ZoneOffset.UTC)
        every { userContactRepository.findByUserId(userId) } returns contact.copy(quietHours = quietHours)
        every { dedupKeyRepository.insertIfAbsent(userId, fingerprint, deduplicationBucketCalculator.bucketFor(occurredAt)) } returns true
        val capturedJob = slot<EmailJob>()
        every { emailJobRepository.save(capture(capturedJob)) } answers { capturedJob.captured }

        val clockInstant = Instant.parse("2024-06-01T23:30:00Z")
        val service = service(clockInstant)
        val outcome = service.notify(alertCommand(severity = "CRITICAL"))

        assertThat(outcome.accepted).isTrue()
        assertThat(outcome.scheduledAt).isEqualTo(clockInstant)
    }

    @Test
    fun `defers email when within quiet hours`() {
        val quietHours = QuietHours(LocalTime.of(22, 0), LocalTime.of(6, 0), ZoneId.of("America/New_York"))
        every { userContactRepository.findByUserId(userId) } returns contact.copy(quietHours = quietHours)
        every { dedupKeyRepository.insertIfAbsent(userId, fingerprint, deduplicationBucketCalculator.bucketFor(occurredAt)) } returns true
        val capturedJob = slot<EmailJob>()
        every { emailJobRepository.save(capture(capturedJob)) } answers { capturedJob.captured }

        val clockInstant = Instant.parse("2024-06-01T03:30:00Z")
        val service = service(clockInstant)
        val outcome = service.notify(alertCommand(severity = "WARN"))

        assertThat(outcome.accepted).isTrue()
        assertThat(outcome.scheduledAt).isEqualTo(Instant.parse("2024-06-01T10:00:00Z"))
        assertThat(capturedJob.captured.scheduledAt).isEqualTo(Instant.parse("2024-06-01T10:00:00Z"))
    }

    @Test
    fun `recommendation respects quiet hours`() {
        val quietHours = QuietHours(LocalTime.of(21, 0), LocalTime.of(7, 0), ZoneOffset.UTC)
        every { userContactRepository.findByUserId(userId) } returns contact.copy(quietHours = quietHours)
        every { dedupKeyRepository.insertIfAbsent(userId, fingerprint, deduplicationBucketCalculator.bucketFor(occurredAt)) } returns true
        val capturedJob = slot<EmailJob>()
        every { emailJobRepository.save(capture(capturedJob)) } answers { capturedJob.captured }

        val clockInstant = Instant.parse("2024-06-01T22:30:00Z")
        val service = service(clockInstant)
        val outcome = service.notify(recommendationCommand(kind = "PORTFOLIO"))

        assertThat(outcome.accepted).isTrue()
        assertThat(outcome.scheduledAt).isEqualTo(Instant.parse("2024-06-02T07:00:00Z"))
        assertThat(capturedJob.captured.scheduledAt).isEqualTo(Instant.parse("2024-06-02T07:00:00Z"))
    }

    private fun service(clockInstant: Instant): NotificationApplicationService =
        NotificationApplicationService(
            dedupKeyRepository = dedupKeyRepository,
            deduplicationBucketCalculator = deduplicationBucketCalculator,
            userContactRepository = userContactRepository,
            emailJobRepository = emailJobRepository,
            clock = Clock.fixed(clockInstant, ZoneOffset.UTC),
            quietHoursScheduler = quietHoursScheduler,
            alertTemplateDataEnricher = alertTemplateDataEnricher,
        )

    private fun alertCommand(severity: String): NotifyAlertCommand =
        NotifyAlertCommand(
            userId = userId,
            fingerprint = fingerprint,
            occurredAt = occurredAt,
            severity = severity,
            templateKey = "template.alert",
            templateData = objectMapper.createObjectNode().put("key", "value"),
        )

    private fun recommendationCommand(kind: String): NotifyRecommendationCommand =
        NotifyRecommendationCommand(
            userId = userId,
            fingerprint = fingerprint,
            occurredAt = occurredAt,
            kind = kind,
            templateKey = "template.reco",
            templateData = objectMapper.createObjectNode().put("item", "value"),
        )

    private fun buildContact(): UserContact =
        UserContact(
            userId = userId,
            primaryEmail = "user@example.com",
            emailStatus = EmailStatus.VERIFIED,
            locale = Locale.US,
            channels = mapOf("email" to true),
            quietHours = null,
            version = 0,
            createdAt = Instant.parse("2024-05-01T00:00:00Z"),
            updatedAt = Instant.parse("2024-05-10T00:00:00Z"),
        )
}
