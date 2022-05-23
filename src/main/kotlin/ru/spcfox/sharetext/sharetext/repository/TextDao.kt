package ru.spcfox.sharetext.sharetext.repository

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ru.spcfox.sharetext.sharetext.exception.PermissionDeniedException
import ru.spcfox.sharetext.sharetext.model.Text
import ru.spcfox.sharetext.sharetext.model.TextExposure
import ru.spcfox.sharetext.sharetext.model.User

@Repository
class TextDao(private val repository: TextRepository) {

    fun getText(textId: Long): Text = repository.getById(textId)

    fun getTextList(page: Int, pageSize: Int): List<Text> =
        repository.getByExposureOrderByCreatedAtDesc(TextExposure.PUBLIC, PageRequest.of(page, pageSize))

    fun getUserTextList(user: User, page: Int, pageSize: Int): List<Text> =
        repository.getByAuthorOrderByCreatedAtDesc(user, PageRequest.of(page, pageSize))

    fun createText(user: User, title: String, body: String, exposure: TextExposure): Text {
        val text = Text(
            title = title,
            body = body,
            author = user,
            exposure = exposure
        )
        return repository.save(text)
    }

    @Transactional
    fun editText(userId: Long, textId: Long, title: String?, body: String?, exposure: TextExposure?): Text {
        val text = repository.getById(textId)
        if (userId != text.author?.userId) {
            throw PermissionDeniedException()
        }
        title?.let { text.title = title }
        body?.let { text.body = body }
        exposure?.let { text.exposure = exposure }
        return text
    }

    @Transactional
    fun delete(userId: Long, textId: Long): Text {
        val text = repository.getById(textId)
        if (userId != text.author?.userId) {
            throw PermissionDeniedException()
        }
        repository.delete(text)
        return text
    }
}