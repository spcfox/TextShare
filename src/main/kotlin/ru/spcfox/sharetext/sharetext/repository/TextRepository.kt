package ru.spcfox.sharetext.sharetext.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ru.spcfox.sharetext.sharetext.model.Text
import ru.spcfox.sharetext.sharetext.model.TextExposure
import ru.spcfox.sharetext.sharetext.model.User

interface TextRepository : JpaRepository<Text, Long> {

    fun getByExposureOrderByCreatedAtDesc(exposure: TextExposure, pageable: Pageable): List<Text>

    fun getByAuthorOrderByCreatedAtDesc(author: User, pageable: Pageable): List<Text>
}
