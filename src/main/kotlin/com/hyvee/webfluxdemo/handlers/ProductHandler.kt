package com.hyvee.webfluxdemo.handlers

import com.hyvee.webfluxdemo.domain.Product
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyToMono
import reactor.core.publisher.Mono
import reactor.core.publisher.switchIfEmpty
import reactor.core.scheduler.Schedulers
import reactor.util.context.Context
import javax.validation.Validator

typealias MdcSetContextMap = (Map<String, String>?) -> Unit

@Component
class ProductHandler(private val validator: Validator) {
    val log = KotlinLogging.logger { }
    val productDb = mutableMapOf("123" to Product("Milk", "123", 2.13))

    fun getProduct(serverRequest: ServerRequest) =
            if (productDb.containsKey(serverRequest.pathVariable("upc"))) {
                ServerResponse.ok().syncBody(productDb[serverRequest.pathVariable("upc")]!!)
            } else {
                ServerResponse.notFound().build()
            }

    fun addProduct(serverRequest: ServerRequest) =
            serverRequest.bodyToMono<Product>().flatMap { product ->
                /**
                 * It would be nice if there was a way to automatically trigger JSR 303/380 validations like in Spring Web.
                 */
                val isInvalid = Mono.fromCallable { validator.validate(product) }.filter { it.isNotEmpty() }

                /**
                 * Complicated conditional logic proved very tricky. Mono#switchIfEmpty can usually be made to work (sometimes
                 * combined with a Mono#filter) with some thinking, but it can be kind of hard to read when you have a lot of
                 * Mono#switchIfEmpty's. Some sort of else-if construct would also be helpful. This also became apparent as
                 * we added more response codes. Every handler ended with a large block of Mono#switchIfEmpty's.
                 */
                isInvalid.flatMap {
                    log.error { it }
                    ServerResponse.badRequest().build()
                }.switchIfEmpty {
                    productDb.putIfAbsent(product.upc, product)

                    val longRunningProcess = Mono.fromCallable { Thread.sleep(3000) }

                    /**
                     * In section 8.8 of the Reactor reference it is mentioned that Logback's MDC is a particular challenge. We ran
                     * into that challenge head on. Not only does the MDC get lost when switching reactive streams, but sometimes
                     * when running in a container we actually got NPE's
                     */
                    longRunningProcess.map {
                        log.info { "This will not log a correlation id" }
                    }
                            .subscribeOn(Schedulers.elastic())
                            /**
                             * Side question: How does one notify a Mono to start emitting data on a separate thread without
                             * calling Mono#subscribe? Calling Mono#subscribe causes a warning about blocking in a non-blocking
                             * scope.
                             */
                            .subscribe()

                    log.info { "This will log a correlation id" }

                    ServerResponse.accepted().build()
                }
            }

    fun updateProduct(serverRequest: ServerRequest): Mono<ServerResponse> {
        val longRunningProcess = Mono.fromCallable { Thread.sleep(3000) }

        /**
         * This is how we originally solved the MDC problem. We ended up using Kotlin extension functions to reduce
         * some of the boilerplate of this approach (see deleteProduct).
         */
        longRunningProcess.flatMap {
            Mono.subscriberContext().map { context: Context ->
                MDC.setContextMap(context["mdc"])
                log.info { "This will log a correlation id" }

            }
        }
                .subscribeOn(Schedulers.elastic())
                .subscriberContext { Context.of("mdc", MDC.getCopyOfContextMap()) }
                .subscribe()

        return ServerResponse.accepted().build()
    }


    fun deleteProduct(serverRequest: ServerRequest): Mono<ServerResponse> {
        val longRunningProcess = Mono.fromCallable { Thread.sleep(3000) }

        /**
         * Using extension functions to reduce boilerplate.
         */
        longRunningProcess.flatMap {
            Mono.subscriberContext().mapInSubscriberContext(MDC::setContextMap) {
                log.info { "This will log a correlation id" }
            }
        }
                .subscribeOn(Schedulers.elastic())
                .setSubscriberContext()
                .subscribe()

        return ServerResponse.accepted().build()
    }

    fun <T> Mono<Context>.mapInSubscriberContext(mdcSetContextMap: MdcSetContextMap, function: () -> T): Mono<T> =
            this.map { ctx ->
                mdcSetContextMap(ctx["mdc"])
                function()
            }

    fun <T> Mono<T>.setSubscriberContext(): Mono<T> =
            this.subscriberContext { Context.of("mdc", MDC.getCopyOfContextMap()) }
}