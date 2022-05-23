package ru.spcfox.sharetext.sharetext.controller

import org.springframework.web.bind.annotation.*
import ru.spcfox.sharetext.sharetext.model.TextExposure
import ru.spcfox.sharetext.sharetext.response.TextIdResponse
import ru.spcfox.sharetext.sharetext.response.TextListResponse
import ru.spcfox.sharetext.sharetext.response.TextResponse
import ru.spcfox.sharetext.sharetext.service.TextService

@RestController
@RequestMapping("/text")
class TextController(private val textService: TextService) {

    @GetMapping("/{textId}")
    fun getText(token: String? = null, @PathVariable textId: String): TextResponse {
        val text = textService.getText(token, textId)
        return TextResponse(text)
    }

    @GetMapping("/list")
    fun getTextList(
        token: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): TextListResponse {
        val textList = textService.getTextList(token, page, pageSize)
        return TextListResponse(textList)
    }

    @GetMapping("/user-list")
    fun getUserTextList(
        token: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): TextListResponse {
        val textList = textService.getUserTextList(token, page, pageSize)
        return TextListResponse(textList)
    }

    @PostMapping("/create")
    fun createText(
        token: String,
        title: String,
        @RequestParam(defaultValue = "PUBLIC") exposure: TextExposure,
        @RequestBody body: String
    ): TextIdResponse {
        val textId = textService.createText(token, title, body, exposure)
        return TextIdResponse(textId)
    }

    @PostMapping("/edit/{textId}")
    fun editText(
        token: String,
        @PathVariable textId: String,
        title: String?,
        exposure: TextExposure?,
        @RequestBody body: String?,
    ): TextIdResponse {
        textService.editText(token, textId, title, body, exposure)
        return TextIdResponse(textId)
    }

    @PostMapping("/delete/{textId}")
    fun deleteText(token: String, @PathVariable textId: String): TextIdResponse {
        textService.deleteText(token, textId)
        return TextIdResponse(textId)
    }
}
