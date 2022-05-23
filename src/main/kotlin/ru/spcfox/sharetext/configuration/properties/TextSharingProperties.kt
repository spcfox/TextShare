package ru.spcfox.sharetext.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("text-sharing")
class TextSharingProperties(
    val maxNameLength: Int,
    val maxTitleLength: Int,
    val maxBodyLength: Int,
    val tokenSaltLength: Int,
    val hashidsSalt: String
)