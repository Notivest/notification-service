package com.notivest.notificationservice.infrastructure.adapters.`in`.web.contact

import com.fasterxml.jackson.databind.ObjectMapper
import com.notivest.notificationservice.application.contact.GetUserContactQuery
import com.notivest.notificationservice.application.contact.UpsertUserContactUseCase
import com.notivest.notificationservice.domain.contact.EmailStatus
import com.notivest.notificationservice.domain.contact.QuietHours
import com.notivest.notificationservice.domain.contact.UserContact
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.contact.dto.QuietHoursDto
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.contact.dto.UpsertUserContactRequest
import com.notivest.notificationservice.infrastructure.mapper.UserContactMapper
import com.notivest.notificationservice.security.JwtUserIdResolver
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

@WebMvcTest(UserContactController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserContactMapper::class, UserContactControllerTest.WebConfig::class)
class UserContactControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    @MockkBean
    private lateinit var getUserContactQuery: GetUserContactQuery

    @MockkBean
    private lateinit var upsertUserContactUseCase: UpsertUserContactUseCase

    @MockkBean
    private lateinit var jwtUserIdResolver: JwtUserIdResolver

    private val userId: UUID = UUID.randomUUID()
    private val jwtAuthenticationToken =
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
            org.springframework.security.oauth2.jwt.Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .claim("iss", "https://issuer.example/")
                .claim("aud", "test-aud")
                .claim("sub", userId.toString())
                .build(),
        )

    @Test
    fun `GET contact returns response when found`() {
        val contact =
            UserContact(
                userId = userId,
                primaryEmail = "user@example.com",
                emailStatus = EmailStatus.VERIFIED,
                locale = null,
                channels = mapOf("email" to true),
                quietHours = null,
                version = 1,
                updatedAt = Instant.parse("2024-06-01T00:00:00Z"),
                createdAt = Instant.parse("2024-05-01T00:00:00Z"),
            )

        every { jwtUserIdResolver.requireUserId(any()) } returns userId
        every { getUserContactQuery.get(userId) } returns contact

        val context = SecurityContextHolder.createEmptyContext().apply {
            authentication = jwtAuthenticationToken
        }
        SecurityContextHolder.setContext(context)

        try {
            mockMvc.perform(get("/api/v1/contact"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.primaryEmail").value("user@example.com"))
                .andExpect(jsonPath("$.version").value(1))
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    @Test
    fun `GET contact returns problem detail when not found`() {
        every { jwtUserIdResolver.requireUserId(any()) } returns userId
        every { getUserContactQuery.get(userId) } returns null

        val context = SecurityContextHolder.createEmptyContext().apply {
            authentication = jwtAuthenticationToken
        }
        SecurityContextHolder.setContext(context)

        try {
            mockMvc.perform(get("/api/v1/contact"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.type").value("urn:problem:user-contact:user-contact-not-found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    @Test
    fun `POST contact upserts and returns payload`() {
        val request =
            UpsertUserContactRequest(
                primaryEmail = "user@example.com",
                emailStatus = EmailStatus.UNVERIFIED,
                locale = "en-US",
                channels = mapOf("email" to true, "push" to false),
                quietHours =
                    QuietHoursDto(
                        start = "22:00",
                        end = "06:30",
                        timezone = "America/New_York",
                    ),
            )
        val saved =
            UserContact(
                userId = userId,
                primaryEmail = request.primaryEmail,
                emailStatus = EmailStatus.UNVERIFIED,
                locale = Locale.forLanguageTag("en-US"),
                channels = request.channels,
                quietHours =
                    QuietHours(
                        start = LocalTime.of(22, 0),
                        end = LocalTime.of(6, 30),
                        timezone = ZoneId.of("America/New_York"),
                    ),
                version = 0,
                updatedAt = Instant.parse("2024-06-01T10:00:00Z"),
                createdAt = Instant.parse("2024-06-01T10:00:00Z"),
            )

        every { jwtUserIdResolver.requireUserId(any()) } returns userId
        every { upsertUserContactUseCase.upsert(any()) } returns saved

        val context = SecurityContextHolder.createEmptyContext().apply {
            authentication = jwtAuthenticationToken
        }
        SecurityContextHolder.setContext(context)

        try {
            mockMvc.perform(
                post("/api/v1/contact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.quietHours.start").value("22:00"))
                .andExpect(jsonPath("$.quietHours.timezone").value("America/New_York"))
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    @TestConfiguration
    class WebConfig : WebMvcConfigurer {
        override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
            resolvers.add(AuthenticationPrincipalArgumentResolver())
        }
    }
}
