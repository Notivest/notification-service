package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.dedup

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "dedup_key")
class DedupKeyEntity(
    @EmbeddedId
    private val dedupKeyId: DedupKeyId = DedupKeyId(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) : Persistable<DedupKeyId> {

    @Transient
    private var newEntity: Boolean = true

    override fun getId(): DedupKeyId = dedupKeyId

    override fun isNew(): Boolean = newEntity

    @PostLoad
    @PostPersist
    fun markNotNew() {
        newEntity = false
    }
}

@Embeddable
data class DedupKeyId(
    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID(0L, 0L),
    @Column(name = "fingerprint", nullable = false, length = 128)
    val fingerprint: String = "",
    @Column(name = "bucket", nullable = false)
    val bucket: Instant = Instant.EPOCH,
)
