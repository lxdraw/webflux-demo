package com.hyvee.webfluxdemo.handlers

import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Component
class ProductHandler {
    val log = KotlinLogging.logger { }

    fun addProduct(serverRequest: ServerRequest): Mono<ServerResponse> {
        val longRunningProcess = Mono.fromCallable {
            log.info { "This will throw an NPE" }
            Thread.sleep(5000)
        }
        longRunningProcess.subscribeOn(Schedulers.immediate()).subscribe()

        return ServerResponse.accepted().build()
    }
}