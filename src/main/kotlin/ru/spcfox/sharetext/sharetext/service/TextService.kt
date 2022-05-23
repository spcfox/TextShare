package ru.spcfox.sharetext.sharetext.service

import org.hashids.Hashids
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import org.springframework.stereotype.Service
import ru.spcfox.sharetext.configuration.properties.TextSharingProperties
import ru.spcfox.sharetext.sharetext.exception.InvalidTextException
import ru.spcfox.sharetext.sharetext.exception.PermissionDeniedException
import ru.spcfox.sharetext.sharetext.exception.TextNotFoundException
import ru.spcfox.sharetext.sharetext.model.Text
import ru.spcfox.sharetext.sharetext.model.TextExposure
import ru.spcfox.sharetext.sharetext.model.TextResponseEntity
import ru.spcfox.sharetext.sharetext.model.User
import ru.spcfox.sharetext.sharetext.repository.TextDao

@Service
class TextService(
    private val jwtService: JwtService,
    private val textDao: TextDao,
    private val hashids: Hashids,
    private val textSharingProperties: TextSharingProperties
) {

    fun getText(token: String?, textHashId: String): TextResponseEntity {
        val textId = decodeHashId(textHashId)
        val user = token?.let { jwtService.getUser(token) }
        val text: Text
        try {
            text = textDao.getText(textId)
        } catch (e: JpaObjectRetrievalFailureException) {
            throw TextNotFoundException(textId.toString())
        }
        val canEdit = hasPermission(user, text)
        if (text.exposure == TextExposure.PRIVATE && !canEdit) {
            throw PermissionDeniedException()
        }
        return TextResponseEntity(text, encodeHashId(textId), canEdit)
    }

    fun getTextList(token: String?, page: Int, pageSize: Int): List<TextResponseEntity> {
        val user = token?.let { jwtService.getUser(token) }
        val texts = textDao.getTextList(page, pageSize)
        return texts.map { text -> TextResponseEntity(text, encodeHashId(text.textId), hasPermission(user, text)) }
    }

    fun getUserTextList(token: String, page: Int, pageSize: Int): List<TextResponseEntity> {
        val user = jwtService.getUser(token)
        val texts = textDao.getUserTextList(user, page, pageSize)
        return texts.map { text -> TextResponseEntity(text, encodeHashId(text.textId), hasPermission(user, text)) }
    }

    fun createText(token: String, title: String, body: String, exposure: TextExposure): String {
        val user = jwtService.getUser(token)
        val text = textDao.createText(user, title.prepareTitle(), body.prepareBody(), exposure)
        return encodeHashId(text.textId)
    }

    fun editText(token: String, textHashId: String, title: String?, body: String?, exposure: TextExposure?): String {
        val textId = decodeHashId(textHashId)
        val user = jwtService.getUser(token)
        try {
            textDao.editText(user.userId, textId, title?.prepareTitle(), body?.prepareTitle(), exposure)
        } catch (e: JpaObjectRetrievalFailureException) {
            throw TextNotFoundException(textHashId)
        }
        return textHashId
    }

    fun deleteText(token: String, textHashId: String): String {
        val textId = decodeHashId(textHashId)
        val user = jwtService.getUser(token)
        try {
            textDao.delete(user.userId, textId)
        } catch (e: JpaObjectRetrievalFailureException) {
            throw TextNotFoundException(textHashId)
        }
        return textHashId
    }

    private fun String.prepareTitle(): String = when {
        isBlank() ->
            throw InvalidTextException("Title is empty.")
        length > textSharingProperties.maxNameLength ->
            throw InvalidTextException("The title is too long. Maximum ${textSharingProperties.maxTitleLength} characters.")
        else -> trim()
    }

    private fun String.prepareBody(): String = when {
        isBlank() ->
            throw InvalidTextException("Body is empty.")
        length > textSharingProperties.maxNameLength ->
            throw InvalidTextException("The body is too long. Maximum ${textSharingProperties.maxBodyLength} characters.")
        else -> trim()
    }

    private fun decodeHashId(hashId: String): Long {
        val ids = hashids.decode(hashId)
        if (ids.size != 1) {
            throw TextNotFoundException(hashId)
        }
        return ids[0]
    }

    private fun encodeHashId(id: Long): String = hashids.encode(id)

    private fun hasPermission(user: User?, text: Text) = user?.userId == text.author?.userId
}
