package com.notivest.notificationservice.observability

import com.notivest.notificationservice.application.emailjob.EmailJobProcessingResult
import com.notivest.notificationservice.application.notification.NotificationOutcome
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component

@Component
class NotificationMetrics(
    private val meterRegistry: MeterRegistry,
) {
    private val notificationTimer: Timer =
        Timer.builder("notification.requests.duration")
            .description("Duration of notification accept/reject flow")
            .register(meterRegistry)

    private val emailJobTimer: Timer =
        Timer.builder("notification.email.jobs.duration")
            .description("Duration of individual email job processing")
            .register(meterRegistry)

    private val workerPollTimer: Timer =
        Timer.builder("notification.email.worker.poll.duration")
            .description("Duration of an email worker polling cycle")
            .register(meterRegistry)

    fun registerBaseMeters() {
        meterRegistry.counter("notification.requests", "outcome", "accepted")
        meterRegistry.counter("notification.requests", "outcome", "rejected", "reason", "CONTACT_NOT_FOUND")
        meterRegistry.counter("notification.requests", "outcome", "rejected", "reason", "EMAIL_CHANNEL_DISABLED")
        meterRegistry.counter("notification.requests", "outcome", "rejected", "reason", "EMAIL_STATUS_BLOCKED")
        meterRegistry.counter("notification.requests", "outcome", "rejected", "reason", "DEDUPLICATED")
        meterRegistry.counter("notification.email.jobs", "outcome", "sent")
        meterRegistry.counter("notification.email.jobs", "outcome", "failed")
        meterRegistry.counter("notification.email.jobs", "outcome", "skipped")
        meterRegistry.counter("notification.email.worker.polls", "outcome", "empty")
        meterRegistry.counter("notification.email.worker.polls", "outcome", "processed")
    }

    fun startNotificationTimer(): Timer.Sample = Timer.start(meterRegistry)

    fun stopNotificationTimer(sample: Timer.Sample) {
        sample.stop(notificationTimer)
    }

    fun recordNotificationOutcome(outcome: NotificationOutcome) {
        val counter =
            if (outcome.accepted) {
                meterRegistry.counter("notification.requests", "outcome", "accepted")
            } else {
                meterRegistry.counter(
                    "notification.requests",
                    "outcome",
                    "rejected",
                    "reason",
                    outcome.reason?.name ?: "UNKNOWN",
                )
            }
        counter.increment()
    }

    fun startEmailJobTimer(): Timer.Sample = Timer.start(meterRegistry)

    fun stopEmailJobTimer(sample: Timer.Sample) {
        sample.stop(emailJobTimer)
    }

    fun recordEmailJobOutcome(outcome: String) {
        meterRegistry.counter("notification.email.jobs", "outcome", outcome).increment()
    }

    fun startWorkerPollTimer(): Timer.Sample = Timer.start(meterRegistry)

    fun stopWorkerPollTimer(sample: Timer.Sample) {
        sample.stop(workerPollTimer)
    }

    fun recordWorkerPoll(result: EmailJobProcessingResult) {
        val outcome = if (result.total == 0) "empty" else "processed"
        meterRegistry.counter("notification.email.worker.polls", "outcome", outcome).increment()
        if (result.total > 0) {
            meterRegistry.counter("notification.email.worker.jobs.total").increment(result.total.toDouble())
        }
    }
}
