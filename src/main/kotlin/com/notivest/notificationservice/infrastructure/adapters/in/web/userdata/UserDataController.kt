package com.notivest.notificationservice.infrastructure.adapters.`in`.web.userdata

import com.notivest.notificationservice.application.userdata.UserDataDeletionService
import com.notivest.notificationservice.security.JwtUserIdResolver
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping
class UserDataController(
    private val userDataDeletionService: UserDataDeletionService,
    private val jwtUserIdResolver: JwtUserIdResolver,
) {
    @DeleteMapping("/v1/user-data/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMyData(
        @AuthenticationPrincipal jwt: Jwt,
    ) {
        val userId = jwtUserIdResolver.requireUserId(jwt)
        userDataDeletionService.deleteUserData(userId)
    }

    @DeleteMapping("/internal/v1/user-data")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUserDataInternal(
        @RequestParam userId: UUID,
    ) {
        userDataDeletionService.deleteUserData(userId)
    }
}

