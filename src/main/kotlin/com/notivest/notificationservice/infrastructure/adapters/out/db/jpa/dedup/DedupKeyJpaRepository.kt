package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.dedup

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DedupKeyJpaRepository : JpaRepository<DedupKeyEntity, DedupKeyId> {
    @Query(value = "select count(*) from dedup_key where user_id = :userId", nativeQuery = true)
    fun countByUserId(
        @Param("userId") userId: UUID,
    ): Long

    @Modifying
    @Query(value = "delete from dedup_key where user_id = :userId", nativeQuery = true)
    fun deleteByUserId(
        @Param("userId") userId: UUID,
    ): Int
}
