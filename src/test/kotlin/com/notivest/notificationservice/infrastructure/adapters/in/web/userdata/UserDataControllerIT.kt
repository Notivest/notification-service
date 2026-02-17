package com.notivest.notificationservice.infrastructure.adapters.`in`.web.userdata

import com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.dedup.DedupKeyEntity
import com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.dedup.DedupKeyId
import com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.dedup.DedupKeyJpaRepository
import com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.emailjob.EmailJobEntity
import com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.emailjob.EmailJobJpaRepository
import com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.usercontact.UserContactEntity
import com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.usercontact.UserContactJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("auth", "test")
@TestPropertySource(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
class UserDataControllerIT {
    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var emailJobJpaRepository: EmailJobJpaRepository

    @Autowired lateinit var dedupKeyJpaRepository: DedupKeyJpaRepository

    @Autowired lateinit var userContactJpaRepository: UserContactJpaRepository

    @MockBean lateinit var jwtDecoder: JwtDecoder

    @BeforeEach
    fun setUp() {
        dedupKeyJpaRepository.deleteAll()
        emailJobJpaRepository.deleteAll()
        userContactJpaRepository.deleteAll()
    }

    @Test
    fun `delete my user data removes only current user rows`() {
        val userOne = UUID.randomUUID()
        val userTwo = UUID.randomUUID()

        seedEmailJob(userOne, "tpl-user-1")
        seedDedupKey(userOne, "fp-user-1")
        seedUserContact(userOne, "user1@example.com")

        seedEmailJob(userTwo, "tpl-user-2")
        seedDedupKey(userTwo, "fp-user-2")
        seedUserContact(userTwo, "user2@example.com")

        mockMvc
            .perform(
                delete("/v1/user-data/me")
                    .with(
                        jwt().jwt { token ->
                            token.claim("claimId", userOne.toString())
                        },
                    ),
            ).andExpect(status().isNoContent)

        assertThat(emailJobJpaRepository.countByUserId(userOne)).isZero()
        assertThat(dedupKeyJpaRepository.countByUserId(userOne)).isZero()
        assertThat(userContactJpaRepository.countByUserId(userOne)).isZero()

        assertThat(emailJobJpaRepository.countByUserId(userTwo)).isEqualTo(1)
        assertThat(dedupKeyJpaRepository.countByUserId(userTwo)).isEqualTo(1)
        assertThat(userContactJpaRepository.countByUserId(userTwo)).isEqualTo(1)
    }

    @Test
    fun `delete my user data requires authentication`() {
        mockMvc
            .perform(delete("/v1/user-data/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `internal delete requires m2m scope`() {
        val userId = UUID.randomUUID()

        mockMvc
            .perform(
                delete("/internal/v1/user-data")
                    .queryParam("userId", userId.toString())
                    .with(jwt()),
            ).andExpect(status().isForbidden)

        mockMvc
            .perform(
                delete("/internal/v1/user-data")
                    .queryParam("userId", userId.toString())
                    .with(
                        jwt().authorities(
                            SimpleGrantedAuthority("SCOPE_portfolio:read:user-context"),
                        ),
                    ),
            ).andExpect(status().isNoContent)
    }

    private fun seedEmailJob(
        userId: UUID,
        templateKey: String,
    ) {
        val now = Instant.now()
        val entity =
            EmailJobEntity().apply {
                id = UUID.randomUUID()
                this.userId = userId
                this.templateKey = templateKey
                templateJson = """{"title":"test"}"""
                status = "PENDING"
                attempts = 0
                scheduledAt = now
                createdAt = now
                updatedAt = now
            }
        emailJobJpaRepository.saveAndFlush(entity)
    }

    private fun seedDedupKey(
        userId: UUID,
        fingerprint: String,
    ) {
        dedupKeyJpaRepository.saveAndFlush(
            DedupKeyEntity(
                dedupKeyId =
                    DedupKeyId(
                        userId = userId,
                        fingerprint = fingerprint,
                        bucket = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES),
                    ),
                createdAt = Instant.now(),
            ),
        )
    }

    private fun seedUserContact(
        userId: UUID,
        email: String,
    ) {
        val now = Instant.now()
        val entity =
            UserContactEntity().apply {
                this.userId = userId
                primaryEmail = email
                emailStatus = "VERIFIED"
                locale = "en"
                channelsJson = """{"email":true}"""
                quietHoursJson = null
                version = 1
                createdAt = now
                updatedAt = now
            }
        userContactJpaRepository.saveAndFlush(entity)
    }
}

