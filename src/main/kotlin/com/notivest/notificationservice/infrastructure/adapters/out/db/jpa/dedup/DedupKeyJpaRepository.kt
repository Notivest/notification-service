package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.dedup

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DedupKeyJpaRepository : JpaRepository<DedupKeyEntity, DedupKeyId>
