package com.hyvee.webfluxdemo.routers

import com.hyvee.webfluxdemo.domain.Product
import com.hyvee.webfluxdemo.handlers.ProductHandler
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient

@Suppress("ClassName")
class ProductRouterTest {
    private val productHandler = ProductHandler();
    private val productRouter = ProductRouter(productHandler)

    private val webTestClient = WebTestClient.bindToRouterFunction(productRouter.routes()).build()

    @Nested
    inner class `Get product by UPC` {
        @Test
        fun `should return ok status`() {
            webTestClient.get().uri("/products/123").exchange().expectStatus().isOk
        }

        @Test
        fun `should return not found status`() {
            webTestClient.get().uri("/products/456").exchange().expectStatus().isNotFound
        }
    }

    @Nested
    inner class `Add new product` {
        @Test
        fun `should return accepted status`() {
            webTestClient
                    .post()
                    .uri("/products")
                    .syncBody(Product("OJ", "456", 3.33))
                    .exchange()
                    .expectStatus()
                    .isAccepted
        }
    }
}