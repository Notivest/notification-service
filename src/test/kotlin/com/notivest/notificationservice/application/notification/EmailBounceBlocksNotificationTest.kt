package com.notivest.notificationservice.application.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.notivest.notificationservice.application.webhook.EmailWebhookApplicationService
import com.notivest.notificationservice.application.webhook.RegisterEmailEventCommand
import com.notivest.notificationservice.domain.contact.EmailStatus
import com.notivest.notificationservice.domain.contact.UserContact
import com.notivest.notificationservice.domain.contact.port.UserContactRepository
import com.notivest.notificationservice.domain.dedup.DeduplicationBucketCalculator
import com.notivest.notificationservice.domain.dedup.port.DedupKeyRepository
import com.notivest.notificationservice.domain.email.EmailEvent
import com.notivest.notificationservice.domain.email.EmailEventKind
import com.notivest.notificationservice.domain.email.port.EmailEventRepository
import com.notivest.notificationservice.domain.emailjob.EmailJob
import com.notivest.notificationservice.domain.emailjob.EmailJobStatus
import com.notivest.notificationservice.domain.emailjob.port.EmailJobRepository
import com.notivest.notificationservice.domain.notification.QuietHoursScheduler
import com.notivest.notificationservice.domain.portfolio.PortfolioHolding
import com.notivest.notificationservice.domain.portfolio.PortfolioHoldingsQuery
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class EmailBounceBlocksNotificationTest {

    private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()
    private val clockInstant: Instant = Instant.parse("2024-06-01T12:00:00Z")
    private val clock: Clock = Clock.fixed(clockInstant, ZoneOffset.UTC)

    private lateinit var dedupRepository: InMemoryDedupKeyRepository
    private lateinit var userContactRepository: InMemoryUserContactRepository
    private lateinit var emailJobRepository: InMemoryEmailJobRepository
    private lateinit var emailEventRepository: InMemoryEmailEventRepository

    private lateinit var notificationService: NotificationApplicationService
    private lateinit var webhookService: EmailWebhookApplicationService
    private lateinit var alertTemplateDataEnricher: AlertTemplateDataEnricher

    private val userId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        dedupRepository = InMemoryDedupKeyRepository()
        userContactRepository = InMemoryUserContactRepository()
        emailJobRepository = InMemoryEmailJobRepository()
        emailEventRepository = InMemoryEmailEventRepository()
        alertTemplateDataEnricher = AlertTemplateDataEnricher(NoOpPortfolioHoldingsQuery(), objectMapper)

        val contact =
            UserContact(
                userId = userId,
                primaryEmail = "user@example.com",
                emailStatus = EmailStatus.VERIFIED,
                locale = null,
                channels = mapOf("email" to true),
                quietHours = null,
                version = 0,
                createdAt = clockInstant.minusSeconds(3600),
                updatedAt = clockInstant.minusSeconds(3600),
            )
        userContactRepository.save(contact)

        notificationService =
            NotificationApplicationService(
                dedupRepository,
                DeduplicationBucketCalculator(Duration.ofMinutes(5)),
                userContactRepository,
                emailJobRepository,
                clock,
                QuietHoursScheduler(),
                alertTemplateDataEnricher,
            )

        webhookService = EmailWebhookApplicationService(emailEventRepository, userContactRepository, clock)
    }

    @Test
    fun `bounce event blocks subsequent notifications`() {
        // Initial notification succeeds
        val firstOutcome = notificationService.notify(sampleAlertCommand())
        assertThat(firstOutcome.accepted).isTrue()

        // Register bounce webhook
        webhookService.register(
            RegisterEmailEventCommand(
                eventId = UUID.randomUUID(),
                userId = userId,
                email = "user@example.com",
                kind = EmailEventKind.BOUNCE,
                providerReference = "provider-123",
                occurredAt = clockInstant,
                payload = objectMapper.createObjectNode().put("reason", "Hard bounce"),
            ),
        )

        // Notification should now be rejected
        val secondOutcome = notificationService.notify(sampleAlertCommand())
        assertThat(secondOutcome.accepted).isFalse()
        assertThat(secondOutcome.reason).isEqualTo(NotificationRejectReason.EMAIL_STATUS_BLOCKED)

        // User contact updated
        val updatedContact = userContactRepository.findByUserId(userId)
        assertThat(updatedContact?.emailStatus).isEqualTo(EmailStatus.BOUNCED)

        // Email event persisted
        assertThat(emailEventRepository.events).hasSize(1)
    }

    private fun sampleAlertCommand(): NotifyAlertCommand =
        NotifyAlertCommand(
            userId = userId,
            fingerprint = "fp-123",
            occurredAt = clockInstant,
            severity = "WARN",
            templateKey = "alert.v1",
            templateData = objectMapper.createObjectNode().put("symbol", "AAPL"),
        )

    private class InMemoryDedupKeyRepository : DedupKeyRepository {
        private val keys = mutableSetOf<Triple<UUID, String, Instant>>()

        override fun insertIfAbsent(userId: UUID, fingerprint: String, bucket: Instant): Boolean =
            keys.add(Triple(userId, fingerprint, bucket))
    }

    private class InMemoryUserContactRepository : UserContactRepository {
        private val contacts = mutableMapOf<UUID, UserContact>()

        override fun findByUserId(userId: UUID): UserContact? = contacts[userId]

        override fun save(contact: UserContact): UserContact {
            contacts[contact.userId] = contact
            return contact
        }
    }

    private class InMemoryEmailJobRepository : EmailJobRepository {
        val jobs = mutableListOf<EmailJob>()

        override fun save(job: EmailJob): EmailJob {
            jobs.add(job.copy(status = EmailJobStatus.PENDING))
            return job
        }

        override fun findDue(now: Instant, limit: Int): List<EmailJob> =
            jobs.filter { it.status == EmailJobStatus.PENDING && !it.scheduledAt.isAfter(now) }
                .take(if (limit > 0) limit else 0)
    }

    private class InMemoryEmailEventRepository : EmailEventRepository {
        val events = mutableListOf<EmailEvent>()

        override fun save(event: EmailEvent): EmailEvent {
            events.add(event)
            return event
        }
    }

    private class NoOpPortfolioHoldingsQuery : PortfolioHoldingsQuery {
        override fun search(userId: UUID, symbols: List<String>): List<PortfolioHolding> = emptyList()
    }
}
