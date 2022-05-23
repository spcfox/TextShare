package ru.spcfox.sharetext.sharetext.service

import org.springframework.stereotype.Service
import ru.spcfox.sharetext.configuration.properties.TextSharingProperties
import ru.spcfox.sharetext.sharetext.exception.InvalidNameException
import ru.spcfox.sharetext.sharetext.model.User
import ru.spcfox.sharetext.sharetext.repository.UserDao

@Service
class UserService(
    private val userDao: UserDao,
    private val jwtService: JwtService,
    private val textSharingProperties: TextSharingProperties
) {

    fun getAccountInfo(token: String): User {
        return jwtService.getUser(token)
    }

    fun createAccount(name: String): String {
        val user = userDao.create(name.prepareName())
        return jwtService.generateUserToken(user)
    }

    fun editAccount(token: String, name: String): User {
        val user = jwtService.getUser(token)
        return userDao.editName(user, name.prepareName())
    }

    fun revokeToken(token: String): String {
        val user = jwtService.getUser(token)
        userDao.generateNewSalt(user)
        return jwtService.generateUserToken(user)
    }

    private fun String.prepareName(): String = when {
        isBlank() ->
            throw InvalidNameException("Name is empty.")
        length > textSharingProperties.maxNameLength ->
            throw InvalidNameException("The name is too long. Maximum ${textSharingProperties.maxNameLength} characters.")
        else -> trim()
    }
}
