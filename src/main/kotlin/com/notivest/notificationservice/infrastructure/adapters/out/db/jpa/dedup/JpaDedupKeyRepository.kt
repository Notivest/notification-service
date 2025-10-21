package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.dedup

import com.notivest.notificationservice.domain.dedup.port.DedupKeyRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
@ConditionalOnBean(DedupKeyJpaRepository::class)
class JpaDedupKeyRepository(
    private val repository: DedupKeyJpaRepository,
) : DedupKeyRepository {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun insertIfAbsent(userId: UUID, fingerprint: String, bucket: Instant): Boolean =
        repository.insertIfAbsent(
            userId = userId,
            fingerprint = fingerprint,
            bucket = bucket,
            createdAt = Instant.now(),
        ) == 1
}
