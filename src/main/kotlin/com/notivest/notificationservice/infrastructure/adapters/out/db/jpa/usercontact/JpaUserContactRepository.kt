package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.usercontact

import com.notivest.notificationservice.domain.contact.UserContact
import com.notivest.notificationservice.domain.contact.port.UserContactRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@ConditionalOnBean(UserContactJpaRepository::class)
class JpaUserContactRepository(
    private val repository: UserContactJpaRepository,
    private val mapper: UserContactEntityMapper,
) : UserContactRepository {
    override fun findByUserId(userId: UUID): UserContact? =
        repository.findById(userId)
            .map(mapper::toDomain)
            .orElse(null)

    override fun save(contact: UserContact): UserContact {
        val entity = mapper.toEntity(contact)
        val persisted = repository.save(entity)
        return mapper.toDomain(persisted)
    }
}
