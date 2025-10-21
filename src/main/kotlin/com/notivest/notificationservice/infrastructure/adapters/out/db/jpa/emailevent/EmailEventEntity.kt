package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.emailevent

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "email_event")
class EmailEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    lateinit var id: UUID

    @Column(name = "user_id")
    var userId: UUID? = null

    @Column(name = "email", nullable = false, length = 320)
    lateinit var email: String

    @Column(name = "kind", nullable = false, length = 24)
    lateinit var kind: String

    @Column(name = "provider_reference", length = 128)
    var providerReference: String? = null

    @Column(name = "payload", columnDefinition = "jsonb")
    var payload: String? = null

    @Column(name = "occurred_at", nullable = false)
    lateinit var occurredAt: Instant

    @Column(name = "received_at", nullable = false)
    lateinit var receivedAt: Instant
}
