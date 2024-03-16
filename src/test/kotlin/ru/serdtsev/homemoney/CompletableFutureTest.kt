package ru.serdtsev.homemoney

import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class CompletableFutureTest {
    @Test
    fun test() {
        val cf1 = CompletableFuture.supplyAsync { 1 }
        val cf2 = CompletableFuture.supplyAsync { 2 }
        val cf3 = CompletableFuture.supplyAsync { 3 }
        CompletableFuture.allOf(cf1, cf2, cf3)
        println(cf1.thenCombine(cf2) { x1, x2 -> x1 + x2 }.join())
    }
}
