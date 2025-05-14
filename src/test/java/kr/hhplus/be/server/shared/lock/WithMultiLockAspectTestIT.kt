package kr.hhplus.be.server.shared.lock

import kr.hhplus.be.server.lock.annotation.WithMultiDistributedLock
import kr.hhplus.be.server.lock.executor.LockType
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest
class WithMultiLockAspectTestIT {

    @Autowired
    lateinit var testService: MultiLockTestService

    @Autowired
    lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `@WithMultiDistributedLock - 트랜잭션 커밋 이후 멀티 락 해제`() {
        val keys = listOf(101L, 102L)
        val transactionTemplate = TransactionTemplate(transactionManager)

        transactionTemplate.execute {
            testService.lockedBusinessLogic(keys)

            // 트랜잭션 중 락 확인
            keys.forEach {
                val value = redisTemplate.opsForValue().get("lock:product:$it")
                assertNotNull(value, "트랜잭션 중에는 락이 존재해야 합니다 - $it")
            }
        }

        // 트랜잭션 커밋 후 락 해제 확인
        keys.forEach {
            val value = redisTemplate.opsForValue().get("lock:product:$it")
            assertNull(value, "커밋 후 락은 해제되어야 합니다 - $it")
        }
    }
}

@Service
class MultiLockTestService {

    @WithMultiDistributedLock(
        keys = ["#productIds.![ 'product:' + #this ]"], // SpEL 표현으로 다중 키 해석
        type = LockType.SPIN,
        waitTimeMillis = 1000,
        leaseTimeMillis = 5000
    )
    fun lockedBusinessLogic(productIds: List<Long>) {
        // 실제 비즈니스 로직은 생략 (트랜잭션 내 실행 여부와 락 획득 검증용)
    }
}