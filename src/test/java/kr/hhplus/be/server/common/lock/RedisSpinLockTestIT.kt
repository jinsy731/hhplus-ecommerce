package kr.hhplus.be.server.common.lock

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class RedisSpinLockTestIT {

    @Autowired
    lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    lateinit var spinLock: RedisSpinLock

    @Test
    fun `락을 성공적으로 획득하고 해제한다`() {
        val key = "lock:test:spin"
        val locked = spinLock.tryLock(key, 1000, 3000)
        assertTrue(locked, "락을 획득해야 함")

        spinLock.unlock(key)
        val value = redisTemplate.opsForValue().get("lock:$key")
        assertNull(value, "해제 후 락이 존재하지 않아야 함")
    }

    @Test
    fun `이미 락이 걸린 상태에서 다른 스레드는 락 획득에 실패해야 한다`() {
        val key = "lock:test:race"

        val acquired = spinLock.tryLock(key, 1000, 5000)
        assertTrue(acquired)

        val result = spinLock.tryLock(key, 500, 3000)
        assertFalse(result, "다른 스레드는 락을 획득하면 안 됨")

        spinLock.unlock(key)
    }

    @Test
    fun `TTL이 지나면 락이 자동 해제되어야 한다`() {
        val key = "lock:test:ttl"
        val locked = spinLock.tryLock(key, 1000, 1000) // 1초짜리 락
        assertTrue(locked)

        Thread.sleep(1500)
        val now = redisTemplate.opsForValue().get("lock:$key")
        assertNull(now, "TTL이 지난 후 락은 자동으로 해제되어야 함")
    }

    @Test
    fun `동시성 환경에서 하나의 스레드만 락을 획득한다`() {
        val key = "lock:test:concurrent"
        val executor = Executors.newFixedThreadPool(10)
        val successCount = AtomicInteger(0)

        val tasks = (1..10).map {
            Callable {
                val result = spinLock.tryLock(key, 500, 2000)
                if (result) {
                    successCount.incrementAndGet()
                    Thread.sleep(500) // simulate work
                    spinLock.unlock(key)
                }
            }
        }

        executor.invokeAll(tasks)
        executor.shutdown()

        assertEquals(1, successCount.get(), "동시성 환경에서 하나의 스레드만 락을 획득해야 함")
    }

    @Test
    fun `멀티락 획득 및 해제 성공`() {
        // given
        val keys = arrayOf("integration-lock1", "integration-lock2")

        // when
        val locked = spinLock.tryMultiLock(keys, waitTimeMillis = 1000, leaseTimeMillis = 3000)

        // then
        locked shouldBe true

        // unlock
        spinLock.unlockMulti(keys)
    }

    @Test
    fun `중복된 멀티락 획득 실패`() {
        // given
        val keys = arrayOf("integration-lock3", "integration-lock4")

        // 첫 번째 락 성공
        val firstLock = spinLock.tryMultiLock(keys, waitTimeMillis = 1000, leaseTimeMillis = 3000)
        firstLock shouldBe true

        // 두 번째 락은 실패해야 함
        val secondLock = spinLock.tryMultiLock(keys, waitTimeMillis = 500, leaseTimeMillis = 1000)
        secondLock shouldBe false

        // cleanup
        spinLock.unlockMulti(keys)
    }

    @Test
    fun `락 획득 시 Redis에 키가 존재하고 해제 시 키가 삭제된다`() {
        // given
        val keys = arrayOf("integration-lock5", "integration-lock6")
        val prefixedKeys = keys.map { "lock:$it" }

        // when
        val locked = spinLock.tryMultiLock(keys, waitTimeMillis = 1000, leaseTimeMillis = 5000)

        // then
        locked shouldBe true
        // Redis에 실제 락 키가 존재하는지 확인
        prefixedKeys.forEach { key ->
            val exists = redisTemplate.hasKey(key)
            exists shouldBe true
        }

        // unlock
        spinLock.unlockMulti(keys)

        // Redis에서 락 키가 제거되었는지 확인
        prefixedKeys.forEach { key ->
            val exists = redisTemplate.hasKey(key)
            exists shouldBe false
        }
    }
}