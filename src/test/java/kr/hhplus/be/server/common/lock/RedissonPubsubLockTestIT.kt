package kr.hhplus.be.server.common.lock

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
}
