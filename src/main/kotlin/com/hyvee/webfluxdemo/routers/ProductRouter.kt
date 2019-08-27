package com.hyvee.webfluxdemo.routers

import com.hyvee.webfluxdemo.handlers.ProductHandler
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.router
import java.util.*

@Component
class ProductRouter(private val productHandler: ProductHandler) {
    @Bean
    fun routes() = router {
        accept(MediaType.APPLICATION_JSON).nest {
            "/products".nest {
                GET("/{upc}", productHandler::getProduct)
                POST("", productHandler::addProduct)
                PUT("", productHandler::updateProduct)
                DELETE("", productHandler::deleteProduct)
            }
        }
    }.filter { request, next ->
        MDC.put("correlationId", UUID.randomUUID().toString())
        next.handle(request)
    }
}