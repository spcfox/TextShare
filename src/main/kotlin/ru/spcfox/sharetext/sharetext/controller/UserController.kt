package ru.spcfox.sharetext.sharetext.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.spcfox.sharetext.sharetext.response.TokenResponse
import ru.spcfox.sharetext.sharetext.response.UserResponse
import ru.spcfox.sharetext.sharetext.service.UserService

@RestController
@RequestMapping("/account")
class UserController(private val userService: UserService) {

    @GetMapping("/info")
    fun accountInfo(token: String): UserResponse {
        val user = userService.getAccountInfo(token)
        return UserResponse(user)
    }

    @PostMapping("/create")
    fun createAccount(name: String): TokenResponse {
        val token = userService.createAccount(name)
        return TokenResponse(token)
    }

    @PostMapping("/edit")
    fun editAccount(token: String, name: String): UserResponse {
        val user = userService.editAccount(token, name)
        return UserResponse(user)
    }

    @PostMapping("/revoke")
    fun revokeToken(token: String): TokenResponse {
        val newToken = userService.revokeToken(token)
        return TokenResponse(newToken)
    }
}