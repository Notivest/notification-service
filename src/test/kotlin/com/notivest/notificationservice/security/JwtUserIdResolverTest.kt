package com.notivest.notificationservice.security

import com.notivest.notificationservice.exceptions.InvalidUserIdException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.Jwt
import java.nio.charset.StandardCharsets
import java.util.UUID

class JwtUserIdResolverTest {

    private val resolver = JwtUserIdResolver("custom_claim")

    @Test
    fun `extractUserId returns UUID from configured claim`() {
        val expected = UUID.randomUUID()
        val jwt = jwt(mapOf("custom_claim" to expected.toString()))

        assertThat(resolver.extractUserId(jwt)).isEqualTo(expected)
    }

    @Test
    fun `extractUserId derives stable UUID when claim is not a UUID`() {
        val rawValue = "auth0|user-123"
        val jwt = jwt(mapOf("custom_claim" to rawValue))

        val derived = resolver.extractUserId(jwt)
        val expected = UUID.nameUUIDFromBytes("notivest:$rawValue".toByteArray(StandardCharsets.UTF_8))

        assertThat(derived).isEqualTo(expected)
    }

    @Test
    fun `extractUserId falls back to legacy user_id claim`() {
        val expected = UUID.randomUUID()
        val jwt = jwt(mapOf("user_id" to expected.toString()))

        assertThat(resolver.extractUserId(jwt)).isEqualTo(expected)
    }

    @Test
    fun `extractUserId falls back to subject`() {
        val subject = "auth0|subject"
        val jwt = jwt(emptyMap(), subject = subject)

        val derived = resolver.extractUserId(jwt)
        val expected = UUID.nameUUIDFromBytes("notivest:$subject".toByteArray(StandardCharsets.UTF_8))
        assertThat(derived).isEqualTo(expected)
    }

    @Test
    fun `requireUserId throws when no identifier is available`() {
        val jwt = jwt()

        assertThrows<InvalidUserIdException> {
            resolver.requireUserId(jwt)
        }
    }

    private fun jwt(
        claims: Map<String, Any?> = emptyMap(),
        subject: String? = null,
    ): Jwt {
        val builder =
            Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("iss", "https://issuer.example/")
                .claim("aud", "test-aud")

        claims.forEach { (key, value) -> builder.claim(key, value) }
        subject?.let { builder.subject(it) }

        return builder.build()
    }
}
