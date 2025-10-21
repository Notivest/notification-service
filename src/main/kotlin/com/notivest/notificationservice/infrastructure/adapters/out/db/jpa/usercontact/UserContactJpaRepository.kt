package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.usercontact

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserContactJpaRepository : JpaRepository<UserContactEntity, UUID>
