package ru.spcfox.sharetext.sharetext.service

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Service
import ru.spcfox.sharetext.configuration.properties.JwtProperties
import ru.spcfox.sharetext.sharetext.exception.InvalidTokenException
import ru.spcfox.sharetext.sharetext.model.User
import ru.spcfox.sharetext.sharetext.repository.UserDao

@Service
class JwtService(
    private val jwtProperties: JwtProperties,
    private val jwtParser: JwtParser,
    private val userDao: UserDao
) {

    fun generateUserToken(user: User): String = Jwts.builder()
        .setClaims(mapOf(
            "sub" to user.userId.toString(),
            "slt" to user.salt))
        .signWith(jwtProperties.key, jwtProperties.algorithm)
        .compact()

    fun getUser(token: String): User {
        try {
            val body = jwtParser.parseClaimsJws(token).body
            val userId = body.subject.toLong()
            val salt = body["slt"]
            val user = userDao.get(userId)
            if (user.salt != salt) {
                throw InvalidTokenException()
            }
            return user
        } catch (e: Exception) {
            when (e) {
                is JwtException, is NumberFormatException -> throw InvalidTokenException(e)
                else -> throw e
            }
        }
    }
}
