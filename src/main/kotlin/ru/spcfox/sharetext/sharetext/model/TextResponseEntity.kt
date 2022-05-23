package ru.spcfox.sharetext.sharetext.model

import java.util.*

class TextResponseEntity(
    val textId: String? = null,
    val title: String? = null,
    val body: String? = null,
    val author: String? = null,
    val exposure: TextExposure? = null,
    val createdAt: Date? = null,
    val editedAt: Date? = null,
    val canEdit: Boolean = false
) {
    constructor(text: Text, textHashId: String, canEdit: Boolean) : this(
        textId = textHashId,
        title = text.title,
        body = text.body,
        author = text.author?.name,
        exposure = text.exposure,
        createdAt = text.createdAt,
        editedAt = text.editedAt,
        canEdit = canEdit
    )
}
