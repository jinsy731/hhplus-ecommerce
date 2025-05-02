package kr.hhplus.be.server.common.lock

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest
class WithDistributedLockIntegrationTest {


    @Autowired
    lateinit var redisTemplate: StringRedisTemplate
    @Autowired
    lateinit var transactionManager: PlatformTransactionManager
    @Autowired
    lateinit var testService: DistributedLockTestService

    @Test
    fun `기본형 파라미터 기반 key가 SpEL로 해석되고 락이 정확히 동작해야 한다`() {
        val userId = 42L
        val expectedRedisKey = "lock:test:lock:$userId"
        val ops = redisTemplate.opsForValue()

        val tx = TransactionTemplate(transactionManager)
        val result = tx.execute {
            val r = testService.criticalOperation(userId)
            val redisValue = ops.get(expectedRedisKey)

            assertEquals("locked:42", r)
            assertNotNull(redisValue, "트랜잭션 중에는 락이 Redis에 존재해야 합니다.")
            r
        }

        assertEquals("locked:42", result)
        assertNull(ops.get(expectedRedisKey), "트랜잭션 커밋 후 락은 해제되어야 합니다.")
    }

    @Test
    fun `계층형 DTO 파라미터에서 SpEL로 key가 해석되고 락이 정확히 동작해야 한다`() {
        val request = RequestDto(UserDto(11), OrderDto(999))
        val expectedRedisKey = "lock:test:lock:11:999"
        val ops = redisTemplate.opsForValue()

        val tx = TransactionTemplate(transactionManager)
        val result = tx.execute {
            val r = testService.complexLock(request)
            val redisValue = ops.get(expectedRedisKey)

            assertEquals("locked:11:999", r)
            assertNotNull(redisValue, "트랜잭션 중에는 락이 Redis에 존재해야 합니다.")
            r
        }

        assertEquals("locked:11:999", result)
        assertNull(ops.get(expectedRedisKey), "트랜잭션 커밋 후 락은 해제되어야 합니다.")
    }
}


@Service
class DistributedLockTestService {

    @WithDistributedLock(
        key = "'test:lock:' + #userId",
        type = LockType.SPIN,
        waitTimeMillis = 1000,
        leaseTimeMillis = 3000
    )
    fun criticalOperation(userId: Long): String {
        return "locked:$userId"
    }

    @WithDistributedLock(
        key = "'test:lock:' + #request.user.id + ':' + #request.order.id",
        type = LockType.SPIN,
        waitTimeMillis = 1000,
        leaseTimeMillis = 3000
    )
    fun complexLock(request: RequestDto): String {
        return "locked:${request.user.id}:${request.order.id}"
    }
}

data class UserDto(val id: Long)
data class OrderDto(val id: Long)
data class RequestDto(val user: UserDto, val order: OrderDto)