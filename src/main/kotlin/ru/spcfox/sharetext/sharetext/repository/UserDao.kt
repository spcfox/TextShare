package ru.spcfox.sharetext.sharetext.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ru.spcfox.sharetext.configuration.properties.TextSharingProperties
import ru.spcfox.sharetext.sharetext.model.User

@Repository
class UserDao(
    private val repository: UserRepository,
    private val textSharingProperties: TextSharingProperties
) {

    private val alphaNumeric = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    fun get(userId: Long) =
        repository.getById(userId)

    fun create(name: String): User {
        val salt = generateSalt()
        val user = User(name = name, salt = salt)
        return repository.save(user)
    }

    @Transactional
    fun editName(user: User, name: String): User {
        user.name = name
        return user
    }

    @Transactional
    fun generateNewSalt(user: User): User {
        var salt: String
        do {
            salt = generateSalt()
        } while (salt == user.salt)
        user.salt = salt
        return user
    }

    private fun generateSalt(): String =
        alphaNumeric.asSequence()
            .shuffled()
            .take(textSharingProperties.tokenSaltLength)
            .joinToString("")
}