package ru.spcfox.sharetext.configuration

import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import org.hashids.Hashids
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.spcfox.sharetext.configuration.properties.JwtProperties
import ru.spcfox.sharetext.configuration.properties.TextSharingProperties

@Configuration
class Configuration(
    private val textSharingProperties: TextSharingProperties,
    private val jwtProperties: JwtProperties
) {

    @Bean
    fun hashids() = Hashids(textSharingProperties.hashidsSalt, 5)

    @Bean
    fun jwtParser(): JwtParser = Jwts.parserBuilder().setSigningKey(jwtProperties.key).build()
}