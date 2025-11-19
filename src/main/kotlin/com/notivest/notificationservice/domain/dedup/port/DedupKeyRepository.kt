package com.notivest.notificationservice.domain.dedup.port

import java.time.Instant
import java.util.UUID

interface DedupKeyRepository {
    fun insertIfAbsent(userId: UUID, fingerprint: String, bucket: Instant): Boolean
}
