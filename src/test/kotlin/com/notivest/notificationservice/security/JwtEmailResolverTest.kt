package com.notivest.notificationservice.security

import com.notivest.notificationservice.exceptions.InvalidUserEmailException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.env.MockEnvironment
import org.springframework.security.oauth2.jwt.Jwt

class JwtEmailResolverTest {

    @Test
    fun `extractEmail returns value from configured claim`() {
        val resolver = resolver()
        val jwt = jwt(mapOf("custom_email" to "user@example.com"))

        assertThat(resolver.extractEmail(jwt)).isEqualTo("user@example.com")
    }

    @Test
    fun `extractEmail returns null when claim is missing`() {
        val resolver = resolver()
        val jwt = jwt()

        assertThat(resolver.extractEmail(jwt)).isNull()
    }

    @Test
    fun `requireEmail throws when missing`() {
        val resolver = resolver()
        val jwt = jwt()

        assertThrows<InvalidUserEmailException> {
            resolver.requireEmail(jwt)
        }
    }

    @Test
    fun `requireEmail falls back to default email when profile active`() {
        val resolver = resolver(defaultEmail = "default@example.com", activeProfiles = arrayOf("default-email"))
        val jwt = jwt()

        assertThat(resolver.requireEmail(jwt)).isEqualTo("default@example.com")
    }

    private fun jwt(claims: Map<String, Any?> = emptyMap()): Jwt {
        val builder =
            Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("iss", "https://issuer.example/")
                .claim("aud", "test-aud")

        claims.forEach { (key, value) -> builder.claim(key, value) }

        return builder.build()
    }

    private fun resolver(
        claim: String = "custom_email",
        defaultEmail: String = "",
        activeProfiles: Array<String> = emptyArray(),
    ): JwtEmailResolver {
        val env =
            MockEnvironment().apply {
                if (activeProfiles.isNotEmpty()) {
                    setActiveProfiles(*activeProfiles)
                }
            }
        return JwtEmailResolver(claim, defaultEmail, env)
    }
}
