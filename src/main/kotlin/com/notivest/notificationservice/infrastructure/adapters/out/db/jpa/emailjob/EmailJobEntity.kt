package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.emailjob

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "email_job")
class EmailJobEntity {

    @Id
    @Column(name = "id", nullable = false)
    lateinit var id: UUID

    @Column(name = "user_id", nullable = false)
    lateinit var userId: UUID

    @Column(name = "template_key", nullable = false, length = 64)
    lateinit var templateKey: String

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_json", nullable = false, columnDefinition = "jsonb")
    lateinit var templateJson: String

    @Column(name = "status", nullable = false, length = 16)
    lateinit var status: String

    @Column(name = "attempts", nullable = false)
    var attempts: Int = 0

    @Column(name = "error")
    var error: String? = null

    @Column(name = "scheduled_at", nullable = false)
    lateinit var scheduledAt: Instant

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: Instant

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: Instant

    @PrePersist
    fun onCreate() {
        val now = Instant.now()
        if (!::createdAt.isInitialized) {
            createdAt = now
        }
        if (!::updatedAt.isInitialized) {
            updatedAt = now
        }
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
