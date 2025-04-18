package kr.hhplus.be.server

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

fun executeThreads(count: Int = 100, block: () -> Any) {
    val countDownLatch = CountDownLatch(count)
    val executor = Executors.newFixedThreadPool(100)

    repeat(count) {
        executor.submit {
            block()
            countDownLatch.countDown()
        }
    }

    countDownLatch.await()
}