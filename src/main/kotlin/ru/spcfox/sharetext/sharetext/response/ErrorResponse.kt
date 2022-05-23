package ru.spcfox.sharetext.sharetext.response

sealed class ErrorResponse(val error: String, val message: String) : Response(false)

class InvalidTokenResponse : ErrorResponse("INVALID_TOKEN", "Invalid token")

class PermissionDeniedResponse : ErrorResponse("PERMISSION_DENIED", "Permission denied")

class InvalidNameResponse(message: String) : ErrorResponse("INVALID_NAME", message)

class InvalidTextResponse(message: String) : ErrorResponse("INVALID_TEXT", message)

class TextNotFoundResponse(message: String) : ErrorResponse("TEXT_NOT_FOUND", message)
