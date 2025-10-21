package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.emailjob

import com.notivest.notificationservice.domain.emailjob.EmailJob
import com.notivest.notificationservice.domain.emailjob.port.EmailJobRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component

@Component
@ConditionalOnBean(EmailJobJpaRepository::class)
class JpaEmailJobRepository(
    private val repository: EmailJobJpaRepository,
    private val mapper: EmailJobEntityMapper,
) : EmailJobRepository {

    override fun save(job: EmailJob): EmailJob {
        val entity = mapper.toEntity(job)
        val saved = repository.save(entity)
        return mapper.toDomain(saved)
    }
}
