package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.emailjob

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EmailJobJpaRepository : JpaRepository<EmailJobEntity, UUID>
