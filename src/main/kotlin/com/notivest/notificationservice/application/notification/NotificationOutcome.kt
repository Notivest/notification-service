package com.notivest.notificationservice.application.notification

import java.time.Instant
import java.util.UUID

data class NotificationOutcome(
    val accepted: Boolean,
    val jobId: UUID? = null,
    val scheduledAt: Instant? = null,
    val reason: NotificationRejectReason? = null,
) {
    init {
        if (accepted) {
            require(reason == null) { "Accepted outcomes must not include a rejection reason" }
        } else {
            require(reason != null) { "Rejected outcomes must include a rejection reason" }
        }
    }

    companion object {
        fun accepted(jobId: UUID, scheduledAt: Instant): NotificationOutcome =
            NotificationOutcome(
                accepted = true,
                jobId = jobId,
                scheduledAt = scheduledAt,
            )

        fun rejected(reason: NotificationRejectReason): NotificationOutcome =
            NotificationOutcome(
                accepted = false,
                reason = reason,
            )
    }
}

enum class NotificationRejectReason {
    DEDUPLICATED,
    CONTACT_NOT_FOUND,
    EMAIL_CHANNEL_DISABLED,
    EMAIL_STATUS_BLOCKED,
}
