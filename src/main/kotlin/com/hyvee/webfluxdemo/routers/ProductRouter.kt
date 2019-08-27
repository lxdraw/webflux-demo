package com.hyvee.webfluxdemo.routers

import com.hyvee.webfluxdemo.domain.Product
import com.hyvee.webfluxdemo.handlers.ProductHandler
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import java.util.*

@Component
class ProductRouter(private val productHandler: ProductHandler) {
    @Bean
    fun routes() = router {
        accept(MediaType.APPLICATION_JSON).nest {
            "/products".nest {
                /**
                 * We would like the ability to do something like:
                 * GET("/{upc}") { @PathVariable upc: String ->
                 *  if (upc == "123") {
                 *      ServerResponse.ok().syncBody(Product("Milk", "123", 2.13))
                 *  } else {
                 *      ServerResponse.notFound().build()
                 *  }
                 * }
                 *
                 * Same holds for query parameters and request bodies
                 */
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