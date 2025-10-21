package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.emailevent

import com.notivest.notificationservice.domain.email.EmailEvent
import com.notivest.notificationservice.domain.email.port.EmailEventRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component

@Component
@ConditionalOnBean(EmailEventJpaRepository::class)
class JpaEmailEventRepository(
    private val repository: EmailEventJpaRepository,
    private val mapper: EmailEventEntityMapper,
) : EmailEventRepository {

    override fun save(event: EmailEvent): EmailEvent {
        val saved = repository.save(mapper.toEntity(event))
        return mapper.toDomain(saved)
    }
}
