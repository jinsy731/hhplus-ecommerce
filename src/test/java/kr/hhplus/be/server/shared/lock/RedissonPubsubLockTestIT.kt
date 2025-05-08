package kr.hhplus.be.server.shared.lock

import kr.hhplus.be.server.lock.infrastructure.RedissonPubsubLock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

@SpringBootTest
class RedissonPubsubLockIntegrationTest {

    @Autowired
    lateinit var lock: RedissonPubsubLock

    @Test
    fun `락 획득 및 해제 - Redisson`() {
        val key = "lock:redisson:basic"

        val acquired = lock.tryLock(key, 1000, 3000)
        assertTrue(acquired, "락을 획득해야 함")

        lock.unlock(key)
    }

    @Test
    fun `락 중복 획득 실패`() {
        val key = "lock:redisson:conflict"

        assertTrue(lock.tryLock(key, 1000, 3000))

        thread { assertFalse(lock.tryLock(key, 500, 3000)) } // Redisson Lock은 재진입 가능하기 때문에 별도의 스레드에서 락 획득을 테스트해야함

        lock.unlock(key)
    }

    @Test
    fun `leaseTime이 지나면 락이 자동 해제되어야 한다`() {
        val key = "lock:redisson:ttl"

        assertTrue(lock.tryLock(key, 1000, 1000)) // 1초 후 자동 해제
        Thread.sleep(1500)

        // 다시 락을 잡을 수 있어야 함
        assertTrue(lock.tryLock(key, 1000, 1000))
        lock.unlock(key)
    }

    @Test
    fun `동시성 환경에서 하나의 요청만 락을 획득한다`() {
        val key = "lock:redisson:concurrent"
        val executor = Executors.newFixedThreadPool(10)
        val successCount = AtomicInteger(0)

        val tasks = (1..10).map {
            Callable {
                if (lock.tryLock(key, 500, 2000)) {
                    successCount.incrementAndGet()
                    Thread.sleep(500)
                    lock.unlock(key)
                }
            }
        }

        executor.invokeAll(tasks)
        executor.shutdown()

        assertEquals(1, successCount.get(), "동시성 환경에서 하나의 요청만 락을 획득해야 함")
    }


    @Test
    fun `멀티락 획득 및 해제`() {
        val keys = arrayOf("multi:lock:1", "multi:lock:2", "multi:lock:3")

        val acquired = lock.tryMultiLock(keys, 1000, 3000)
        assertTrue(acquired, "모든 키에 대해 락을 획득해야 함")

        lock.unlockMulti(keys)
    }

    @Test
    fun `하나의 키라도 이미 락이 걸려 있다면 멀티락 획득 실패`() {
        val keys = arrayOf("multi:lock:conflict:1", "multi:lock:conflict:2")

        // 하나의 키에 선점 락
        assertTrue(lock.tryLock(keys[0], 1000, 5000))

        thread {
            val acquired = lock.tryMultiLock(keys, 500, 3000)
            assertFalse(acquired, "하나의 키라도 이미 락이 걸려 있으면 전체 멀티락 실패해야 함")
        }

        lock.unlock(keys[0])
    }

    @Test
    fun `멀티락 해제 후 다시 락 획득 가능`() {
        val keys = arrayOf("multi:lock:reacquire:1", "multi:lock:reacquire:2")

        assertTrue(lock.tryMultiLock(keys, 1000, 2000))
        lock.unlockMulti(keys)

        // 락 해제 후 다시 시도
        assertTrue(lock.tryMultiLock(keys, 1000, 2000))
        lock.unlockMulti(keys)
    }

    @Test
    fun `멀티락에서 하나의 요청만 락을 획득해야 함 (동시성)`() {
        val keys = arrayOf("multi:lock:concurrent:1", "multi:lock:concurrent:2")
        val executor = Executors.newFixedThreadPool(10)
        val successCount = AtomicInteger(0)

        val tasks = (1..10).map {
            Callable {
                if (lock.tryMultiLock(keys, 500, 3000)) {
                    successCount.incrementAndGet()
                    Thread.sleep(1000)
                    lock.unlockMulti(keys)
                }
            }
        }

        executor.invokeAll(tasks)
        executor.shutdown()

        assertEquals(1, successCount.get(), "동시성 환경에서 하나의 요청만 멀티락을 획득해야 함")
    }
}
