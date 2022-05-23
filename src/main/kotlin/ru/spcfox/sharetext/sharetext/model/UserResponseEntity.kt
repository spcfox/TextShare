package ru.spcfox.sharetext.sharetext.model

class UserResponseEntity(
    val userId: Long,
    val name: String
) {
    constructor(user: User) : this(user.userId, user.name)
}
