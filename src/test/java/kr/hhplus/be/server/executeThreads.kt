package kr.hhplus.be.server

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
    executor.shutdown()
}

/**
 * 인덱스와 CountDownLatch를 파라미터로 전달하는 함수
 * 주로 동시성 테스트에서 각 스레드에게 인덱스를 전달하고 싶을 때 사용
 * @param count 실행할 스레드 수
 * @param block 각 스레드에서 실행할 로직을 정의한 함수 (인덱스와 CountDownLatch를 파라미터로 받음)
 */
fun executeThreads(count: Int = 100, block: (index: Int, latch: CountDownLatch) -> Unit) {
    val countDownLatch = CountDownLatch(count)
    val executor = Executors.newFixedThreadPool(100)

    repeat(count) { index ->
        executor.submit {
            try {
                block(index, countDownLatch)
            } catch (e: Exception) {
                countDownLatch.countDown()
                throw e
            }
        }
    }

    countDownLatch.await(30, TimeUnit.SECONDS)
    executor.shutdown()
    executor.awaitTermination(10, TimeUnit.SECONDS)
}