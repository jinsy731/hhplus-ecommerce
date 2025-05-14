package kr.hhplus.be.server.shared.lock

import io.mockk.every
import io.mockk.mockk
import kr.hhplus.be.server.lock.utils.LockKeyResolver
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import kotlin.reflect.jvm.javaMethod

class LockKeyResolverTest {

    private val resolver = LockKeyResolver()

    class DummyService {
        fun singleParamMethod(userId: Long) {}
        fun multiParamMethod(userId: Long, orderId: String) {}
        fun listParamMethod(userIds: List<Item>) {}
    }
    data class Item(val itemId: Long)

    private fun mockJoinPoint(target: Any, method: Method, args: Array<Any?>): JoinPoint {
        val joinPoint = mockk<JoinPoint>()
        val methodSignature = mockk<MethodSignature>()

        every { joinPoint.signature } returns methodSignature
        every { methodSignature.parameterNames } returns method.parameters.map { it.name }.toTypedArray()
        every { joinPoint.args } returns args
        every { joinPoint.target } returns target
        every { methodSignature.method } returns method

        return joinPoint
    }

    @Test
    fun `단일 키 해석 - 기본형 파라미터`() {
        val target = DummyService()
        val method = target::singleParamMethod.javaMethod!!
        val joinPoint = mockJoinPoint(target, method, arrayOf(42L))

        val key = resolver.resolveKey(joinPoint, "'user:' + #userId").first()
        assertEquals("user:42", key)
    }

    @Test
    fun `단일 키 해석 - 여러 파라미터`() {
        val target = DummyService()
        val method = target::multiParamMethod.javaMethod!!
        val joinPoint = mockJoinPoint(target, method, arrayOf(42L, "ORD123"))

        val key = resolver.resolveKey(joinPoint, "'lock:' + #userId + ':' + #orderId").first()
        assertEquals("lock:42:ORD123", key)
    }

    @Test
    fun `다중 키 해석 - List DTO 타입에서 안전하게 키 생성`() {
        val target = DummyService()
        val method = target::listParamMethod.javaMethod!!
        val input = listOf(Item(1L), Item(2L), Item(3L))
        val joinPoint = mockJoinPoint(target, method, arrayOf(input))

        val keys = resolver.resolveKey(joinPoint, "#userIds.![ 'lock:' + #this.itemId ]")
        assertEquals(listOf("lock:1", "lock:2", "lock:3"), keys)
    }

    @Test
    fun `다중 키 해석 - List Long 타입에서 안전하게 키 생성`() {
        val target = DummyService()
        val method = target::listParamMethod.javaMethod!!
        val input = listOf(1L, 2L, 3L)
        val joinPoint = mockJoinPoint(target, method, arrayOf(input))

        val keys = resolver.resolveKey(joinPoint, "#userIds.![ 'lock:' + #this ]")
        assertEquals(listOf("lock:1", "lock:2", "lock:3"), keys)
    }

    @Test
    fun `다중 키 해석 - 중복 제거`() {
        val target = DummyService()
        val method = target::listParamMethod.javaMethod!!
        val input = listOf(1L, 1L, 1L)
        val joinPoint = mockJoinPoint(target, method, arrayOf(input))

        val keys = resolver.resolveKey(joinPoint, "#userIds.![ 'lock:' + #this ]")
        assertEquals(keys.size, 1)
        assertEquals(listOf("lock:1"), keys)
    }

    @Test
    fun `다중 키 해석 - 오름차순 정렬`() {
        val target = DummyService()
        val method = target::listParamMethod.javaMethod!!
        val input = listOf(3L, 1L, 2L)
        val joinPoint = mockJoinPoint(target, method, arrayOf(input))

        val keys = resolver.resolveKey(joinPoint, "#userIds.![ 'lock:' + #this ]")
        assertEquals(listOf("lock:1", "lock:2", "lock:3"), keys)
    }

    @Test
    fun `표현식에 #이 없으면 그대로 반환`() {
        val target = DummyService()
        val method = target::singleParamMethod.javaMethod!!
        val joinPoint = mockJoinPoint(target, method, arrayOf(999L))

        val key = resolver.resolveKey(joinPoint, "static-key").first()
        assertEquals("static-key", key)
    }
}
