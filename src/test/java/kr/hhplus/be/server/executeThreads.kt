package kr.hhplus.be.server

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

fun executeConcurrently(count: Int = 100, block: (Int) -> Any) {
    val countDownLatch = CountDownLatch(count)
    val executor = Executors.newFixedThreadPool(12)

    repeat(count) { it ->
        executor.submit {
            try {
                block(it)
            } catch(e: Throwable) {
                e.printStackTrace()
            } finally {
                countDownLatch.countDown()
            }
        }
    }

    countDownLatch.await()
    executor.shutdown()
}

fun executeMultipleFunctionConcurrently(count: Int = 100, blocks: List<() -> Any>) {
    val countDownLatch = CountDownLatch(count)
    val executor = Executors.newFixedThreadPool(12)

    repeat(count) {
        executor.submit {
            try {
                blocks.random()
            } finally {
                countDownLatch.countDown()
            }
        }
    }

    countDownLatch.await()
    executor.shutdown()
}