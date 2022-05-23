package ru.spcfox.sharetext.sharetext.response

import ru.spcfox.sharetext.sharetext.model.TextResponseEntity
import ru.spcfox.sharetext.sharetext.model.User
import ru.spcfox.sharetext.sharetext.model.UserResponseEntity

sealed class SuccessResponse(open val result: Any) : Response(true)

class TokenResponse(override val result: String) : SuccessResponse(result)

class UserResponse(override val result: UserResponseEntity) : SuccessResponse(result) {
    constructor(user: User) : this(UserResponseEntity(user))
}

class TextIdResponse(override val result: String) : SuccessResponse(result)

class TextResponse(override val result: TextResponseEntity) : SuccessResponse(result)

class TextListResponse(override val result: List<TextResponseEntity>) : SuccessResponse(result)
