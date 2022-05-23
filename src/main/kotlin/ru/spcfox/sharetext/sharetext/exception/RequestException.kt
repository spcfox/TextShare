package ru.spcfox.sharetext.sharetext.exception

sealed class RequestException : RuntimeException {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)
}

class InvalidTokenException : RequestException {
    constructor() : super()
    constructor(cause: Throwable) : super(cause)
}

class PermissionDeniedException : RequestException()

class InvalidNameException(message: String) : RequestException(message)

class InvalidTextException(message: String) : RequestException(message)

class TextNotFoundException(textId: String) : RequestException("Text with id $textId not found")
