package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.usercontact

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_contact")
class UserContactEntity {
    @Id
    @Column(name = "user_id", nullable = false)
    lateinit var userId: UUID

    @Column(name = "primary_email", nullable = false, length = 320)
    lateinit var primaryEmail: String

    @Column(name = "email_status", nullable = false, length = 12)
    lateinit var emailStatus: String

    @Column(name = "locale", length = 10)
    var locale: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "channels_json", nullable = false, columnDefinition = "jsonb")
    lateinit var channelsJson: String

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quiet_hours", columnDefinition = "jsonb")
    var quietHoursJson: String? = null

    @Column(name = "version", nullable = false)
    var version: Long = 0

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: Instant

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: Instant
}
