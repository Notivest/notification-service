package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.dedup

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface DedupKeyJpaRepository : JpaRepository<DedupKeyEntity, DedupKeyId> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            INSERT INTO dedup_key (user_id, fingerprint, bucket, created_at)
            VALUES (:userId, :fingerprint, :bucket, :createdAt)
            ON CONFLICT (bucket, user_id, fingerprint) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("userId") userId: UUID,
        @Param("fingerprint") fingerprint: String,
        @Param("bucket") bucket: Instant,
        @Param("createdAt") createdAt: Instant,
    ): Int
}
