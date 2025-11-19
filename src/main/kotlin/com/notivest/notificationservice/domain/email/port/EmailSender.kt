package com.notivest.notificationservice.domain.email.port

data class OutboundEmail(
    val from: String,
    val to: String,
    val subject: String,
    val body: String,
)

interface EmailSender {
    fun send(email: OutboundEmail)
}
