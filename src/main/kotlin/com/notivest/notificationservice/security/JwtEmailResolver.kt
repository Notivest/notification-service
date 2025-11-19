package com.notivest.notificationservice.security

import com.notivest.notificationservice.exceptions.InvalidUserEmailException
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class JwtEmailResolver(
    @Value("\${JWT_EMAIL_CLAIM:email}")
    private val emailClaim: String,
    @Value("\${notification.security.default-email:}")
    private val defaultEmail: String,
    private val environment: Environment,
) {
    fun requireEmail(jwt: Jwt): String =
        extractEmail(jwt)
            ?: resolveDefaultEmail()
            ?: throw InvalidUserEmailException("JWT missing email claim")

    fun extractEmail(jwt: Jwt): String? =
        jwt.getClaimAsString(emailClaim)?.takeIf { it.isNotBlank() }

    private fun resolveDefaultEmail(): String? {
        if (defaultEmail.isBlank()) {
            return null
        }
        val profileForDefault = Profiles.of("default-email")
        return if (environment.acceptsProfiles(profileForDefault)) defaultEmail else null
    }
}
