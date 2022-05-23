package ru.spcfox.sharetext.configuration.properties

import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import javax.crypto.SecretKey

@ConstructorBinding
@ConfigurationProperties("jwt")
class JwtProperties(
    secret: String
) {
    val algorithm: SignatureAlgorithm = SignatureAlgorithm.HS256
    val key: SecretKey = Keys.hmacShaKeyFor(secret.encodeToByteArray())
}
