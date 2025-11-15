package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.emailjob

import com.notivest.notificationservice.domain.emailjob.EmailJob
import com.notivest.notificationservice.domain.emailjob.EmailJobStatus
import com.notivest.notificationservice.domain.emailjob.port.EmailJobRepository
import org.springframework.stereotype.Repository
import org.springframework.data.domain.PageRequest
import java.time.Instant

@Repository
class JpaEmailJobRepository(
    private val repository: EmailJobJpaRepository,
    private val mapper: EmailJobEntityMapper,
) : EmailJobRepository {

    override fun save(job: EmailJob): EmailJob {
        val entity = mapper.toEntity(job)
        val saved = repository.save(entity)
        return mapper.toDomain(saved)
    }

    override fun findDue(now: Instant, limit: Int): List<EmailJob> {
        if (limit <= 0) {
            return emptyList()
        }
        val pageable = PageRequest.of(0, limit)
        return repository
            .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                EmailJobStatus.PENDING.name,
                now,
                pageable,
            ).map(mapper::toDomain)
    }
}
