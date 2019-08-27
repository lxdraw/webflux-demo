package com.hyvee.webfluxdemo.routers

import com.hyvee.webfluxdemo.domain.Product
import com.hyvee.webfluxdemo.handlers.ProductHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import javax.validation.ConstraintViolation
import javax.validation.Validator

@Suppress("ClassName")
class ProductRouterTest {
    private val validator: Validator = mockk()
    private val productHandler = ProductHandler(validator)
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
        @BeforeEach
        fun setup() {
            every { validator.validate(any<Product>()) } returns emptySet<ConstraintViolation<Product>>()
        }

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

        @Nested
        inner class `Add an invalid new product` {
            private val constraintViolation: ConstraintViolation<Product> = mockk()

            @BeforeEach
            fun setup() {
                every { validator.validate(any<Product>()) } returns setOf<ConstraintViolation<Product>>(constraintViolation)
            }

            @Test
            fun `should return bad request status`() {
                webTestClient
                        .post()
                        .uri("/products")
                        .syncBody(Product("", "456", 3.33))
                        .exchange()
                        .expectStatus()
                        .isBadRequest
            }
        }
    }
}