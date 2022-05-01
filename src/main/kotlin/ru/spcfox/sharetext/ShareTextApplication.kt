package ru.spcfox.sharetext

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ShareTextApplication

fun main(args: Array<String>) {
    runApplication<ShareTextApplication>(*args)
}
