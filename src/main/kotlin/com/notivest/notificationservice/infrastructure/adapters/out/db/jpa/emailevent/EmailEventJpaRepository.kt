package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.emailevent

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EmailEventJpaRepository : JpaRepository<EmailEventEntity, UUID>
