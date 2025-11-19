package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.emailjob

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID
import java.time.Instant

interface EmailJobJpaRepository : JpaRepository<EmailJobEntity, UUID> {
    fun findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
        status: String,
        scheduledAt: Instant,
        pageable: Pageable,
    ): List<EmailJobEntity>
}
