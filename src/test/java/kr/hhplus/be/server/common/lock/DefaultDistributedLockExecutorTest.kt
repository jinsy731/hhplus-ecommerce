package kr.hhplus.be.server.common.lock

import io.kotest.assertions.throwables.shouldThrowExactly
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultDistributedLockExecutorTest {

    private lateinit var executor: DefaultDistributedLockExecutor
    private lateinit var mockLock: DistributedLock

    @BeforeEach
    fun setup() {
        mockLock = mockk(relaxed = true)
        every { mockLock.supports(any()) } returns true
        every { mockLock.tryLock(any(), any(), any()) } returns true
        executor = DefaultDistributedLockExecutor(listOf(mockLock))
    }

    @Test
    fun `락을 정상적으로 획득하고 즉시 해제한다 - 트랜잭션이 없을 경우`() {
        val result = executor.execute("key", LockType.PUBSUB, 3000, 1000) {
            "done"
        }

        assertEquals("done", result)
        verify { mockLock.tryLock("key", 1000, 3000) }
        verify { mockLock.unlock("key") }
    }

    @Test
    fun `락 획득에 실패하면 예외가 발생한다`() {
        every { mockLock.tryLock(any(), any(), any()) } returns false

        val ex = shouldThrowExactly<IllegalStateException> {
            executor.execute("key", LockType.PUBSUB, 1000, 500) { "fail" }
        }

        assertTrue(ex.message!!.contains("Lock acquisition failed"))
        verify { mockLock.tryLock("key", 500, 1000) }
    }


    @Test
    fun `지원하지 않는 락 타입일 경우 예외가 발생한다`() {
        every { mockLock.supports(any()) } returns false

        val ex = shouldThrowExactly<IllegalArgumentException> {
            executor.execute("key", LockType.SPIN, 1000, 500) { "fail" }
        }

        assertTrue(ex.message!!.contains("Not supported LockType"))
    }
}
