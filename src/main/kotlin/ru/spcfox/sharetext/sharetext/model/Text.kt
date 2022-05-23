package ru.spcfox.sharetext.sharetext.model

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.UpdateTimestamp
import java.util.*
import javax.persistence.*

@Entity
@DynamicUpdate
@Table(name = "texts")
class Text(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "text_id", nullable = false, insertable = false, updatable = false)
    val textId: Long = 0,

    @Column(name = "title", nullable = false)
    var title: String? = null,

    @Column(name = "body", nullable = false)
    var body: String? = null,

    @OneToOne
    @JoinColumn(name = "author_id", referencedColumnName = "user_id", nullable = false)
    val author: User? = null,

    @Enumerated(value = EnumType.ORDINAL)
    @Column(name = "exposure", nullable = false)
    var exposure: TextExposure? = null,

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, insertable = true, updatable = false)
    val createdAt: Date? = null,

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "edited_at", nullable = false, insertable = true, updatable = true)
    val editedAt: Date? = null
)

enum class TextExposure {
    PUBLIC, UNLISTED, PRIVATE
}