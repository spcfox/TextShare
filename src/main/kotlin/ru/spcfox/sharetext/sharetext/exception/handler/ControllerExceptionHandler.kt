package ru.spcfox.sharetext.sharetext.exception.handler

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.spcfox.sharetext.sharetext.exception.*
import ru.spcfox.sharetext.sharetext.response.*

@RestControllerAdvice
class ControllerExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler
    @ResponseStatus(HttpStatus.OK)
    fun handleInvalidTokenException(e: RequestException): ErrorResponse = when (e) {
        is InvalidTokenException -> InvalidTokenResponse()
        is PermissionDeniedException -> PermissionDeniedResponse()
        is InvalidNameException -> InvalidNameResponse(e.message ?: "")
        is InvalidTextException -> InvalidTextResponse(e.message ?: "")
        is TextNotFoundException -> TextNotFoundResponse(e.message ?: "")
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentException(e: IllegalArgumentException): Map<String, String> {
        log.warn(e.message, e)
        return errorResponse(e)
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(e: Exception): Map<String, String> {
        log.error(e.message, e)
        return errorResponse(e)
    }

    private fun errorResponse(e: Exception): Map<String, String> = mapOf(
        "status" to "error",
        "exception" to e.javaClass.simpleName,
        "message" to e.message.orEmpty()
    )
}
