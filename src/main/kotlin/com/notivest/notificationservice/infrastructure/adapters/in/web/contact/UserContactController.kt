package com.notivest.notificationservice.infrastructure.adapters.`in`.web.contact

import com.notivest.notificationservice.application.contact.GetUserContactQuery
import com.notivest.notificationservice.application.contact.UpsertUserContactUseCase
import com.notivest.notificationservice.exceptions.InvalidUserEmailException
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.contact.dto.UpsertUserContactRequest
import com.notivest.notificationservice.infrastructure.adapters.`in`.web.contact.dto.UserContactResponse
import com.notivest.notificationservice.infrastructure.mapper.UserContactMapper
import com.notivest.notificationservice.security.JwtEmailResolver
import com.notivest.notificationservice.security.JwtUserIdResolver
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/v1/contact")
class UserContactController(
    private val getUserContactQuery: GetUserContactQuery,
    private val upsertUserContactUseCase: UpsertUserContactUseCase,
    private val mapper: UserContactMapper,
    private val jwtUserIdResolver: JwtUserIdResolver,
    private val jwtEmailResolver: JwtEmailResolver,
) {
    @GetMapping
    fun getContact(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Any> {
        val userId = resolveUserId(jwt)
        val contact = getUserContactQuery.get(userId)

        return if (contact != null) {
            ResponseEntity.ok(mapper.toResponse(contact))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(problemDetail(HttpStatus.NOT_FOUND, "user-contact-not-found", "User contact not found"))
        }
    }

    @PostMapping
    fun upsertContact(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: UpsertUserContactRequest,
    ): ResponseEntity<Any> {
        val userId = resolveUserId(jwt)
        val primaryEmail =
            try {
                jwtEmailResolver.requireEmail(jwt)
            } catch (ex: InvalidUserEmailException) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(problemDetail(HttpStatus.BAD_REQUEST, "email-missing", ex.message ?: "JWT missing email claim"))
            }
        val command = mapper.toCommand(request, userId, primaryEmail)
        val saved = upsertUserContactUseCase.upsert(command)
        return ResponseEntity.ok(mapper.toResponse(saved))
    }

    private fun resolveUserId(jwt: Jwt): UUID = jwtUserIdResolver.requireUserId(jwt)

    private fun problemDetail(status: HttpStatus, type: String, detail: String): ProblemDetail =
        ProblemDetail
            .forStatusAndDetail(status, detail)
            .apply {
                title = status.reasonPhrase
                this.type = URI.create("urn:problem:user-contact:$type")
            }
}
