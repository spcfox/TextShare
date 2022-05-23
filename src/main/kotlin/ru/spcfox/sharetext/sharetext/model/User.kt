package ru.spcfox.sharetext.sharetext.model

import javax.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    val userId: Long = 0,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "salt", nullable = false)
    var salt: String
)
