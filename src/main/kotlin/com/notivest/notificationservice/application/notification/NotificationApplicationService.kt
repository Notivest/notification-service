package com.notivest.notificationservice.application.notification

import com.fasterxml.jackson.databind.JsonNode
import com.notivest.notificationservice.observability.NotificationMetrics
import com.notivest.notificationservice.domain.contact.EmailStatus
import com.notivest.notificationservice.domain.contact.UserContact
import com.notivest.notificationservice.domain.dedup.DeduplicationBucketCalculator
import com.notivest.notificationservice.domain.dedup.port.DedupKeyRepository
import com.notivest.notificationservice.domain.emailjob.EmailJob
import com.notivest.notificationservice.domain.emailjob.port.EmailJobRepository
import com.notivest.notificationservice.domain.notification.QuietHoursScheduler
import com.notivest.notificationservice.domain.contact.port.UserContactRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class NotificationApplicationService(
    private val dedupKeyRepository: DedupKeyRepository,
    private val deduplicationBucketCalculator: DeduplicationBucketCalculator,
    private val userContactRepository: UserContactRepository,
    private val emailJobRepository: EmailJobRepository,
    private val clock: Clock,
    private val quietHoursScheduler: QuietHoursScheduler,
    private val alertTemplateDataEnricher: AlertTemplateDataEnricher,
    private val notificationMetrics: NotificationMetrics,
) : NotifyAlertUseCase, NotifyRecommendationUseCase {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun notify(command: NotifyAlertCommand): NotificationOutcome =
        handleNotification(
            userId = command.userId,
            fingerprint = command.fingerprint,
            occurredAt = command.occurredAt,
            templateKey = command.templateKey,
            templateData = alertTemplateDataEnricher.enrich(command.userId, command.templateData),
            bypassQuietHours = command.severity.equals("CRITICAL", ignoreCase = true),
        )

    override fun notify(command: NotifyRecommendationCommand): NotificationOutcome =
        handleNotification(
            userId = command.userId,
            fingerprint = command.fingerprint,
            occurredAt = command.occurredAt,
            templateKey = command.templateKey,
            templateData = command.templateData,
            bypassQuietHours = false,
        )

    private fun handleNotification(
        userId: UUID,
        fingerprint: String,
        occurredAt: Instant,
        templateKey: String,
        templateData: JsonNode,
        bypassQuietHours: Boolean,
    ): NotificationOutcome {
        val sample = notificationMetrics.startNotificationTimer()
        try {
            val contact = userContactRepository.findByUserId(userId)
                ?: return rejected(userId, fingerprint, NotificationRejectReason.CONTACT_NOT_FOUND)

            if (!isEmailChannelEnabled(contact)) {
                return rejected(userId, fingerprint, NotificationRejectReason.EMAIL_CHANNEL_DISABLED)
            }

            if (isEmailStatusBlocked(contact.emailStatus)) {
                return rejected(userId, fingerprint, NotificationRejectReason.EMAIL_STATUS_BLOCKED)
            }

            val bucket = deduplicationBucketCalculator.bucketFor(occurredAt)
            val inserted = dedupKeyRepository.insertIfAbsent(userId, fingerprint, bucket)
            if (!inserted) {
                return rejected(userId, fingerprint, NotificationRejectReason.DEDUPLICATED)
            }

            val now = Instant.now(clock)
            val scheduledAt = quietHoursScheduler.schedule(now, contact.quietHours, bypassQuietHours)
            val job =
                EmailJob.pending(
                    userId = userId,
                    templateKey = templateKey,
                    templateData = templateData,
                    scheduledAt = scheduledAt,
                    createdAt = now,
                )
            val saved = emailJobRepository.save(job)
            val outcome = NotificationOutcome.accepted(saved.id, saved.scheduledAt)
            notificationMetrics.recordNotificationOutcome(outcome)
            logger.info(
                "notification.accepted userId={} fingerprint={} jobId={} scheduledAt={} bypassQuietHours={}",
                userId,
                fingerprint,
                saved.id,
                saved.scheduledAt,
                bypassQuietHours,
            )
            return outcome
        } finally {
            notificationMetrics.stopNotificationTimer(sample)
        }
    }

    private fun isEmailChannelEnabled(contact: UserContact): Boolean =
        contact.channels["email"] == true

    private fun isEmailStatusBlocked(status: EmailStatus): Boolean =
        status == EmailStatus.BOUNCED || status == EmailStatus.UNSUB

    private fun rejected(
        userId: UUID,
        fingerprint: String,
        reason: NotificationRejectReason,
    ): NotificationOutcome {
        val outcome = NotificationOutcome.rejected(reason)
        notificationMetrics.recordNotificationOutcome(outcome)
        logger.info("notification.rejected userId={} fingerprint={} reason={}", userId, fingerprint, reason)
        return outcome
    }
}
