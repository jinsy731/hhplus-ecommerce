package kr.hhplus.be.server.common.lock

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.redisson.RedissonMultiLock
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

@Aspect
@Component
class WithMultiDistributedLockAspect(
    private val redissonClient: RedissonClient
) {
    private val parser = SpelExpressionParser()
    private val paramNameDiscoverer = DefaultParameterNameDiscoverer()

    @Around("@annotation(multiLock)")
    fun around(joinPoint: ProceedingJoinPoint, multiLock: WithMultiDistributedLock): Any? {
        val method = (joinPoint.signature as org.aspectj.lang.reflect.MethodSignature).method
        val paramNames = paramNameDiscoverer.getParameterNames(method) ?: emptyArray()
        val args = joinPoint.args
        val context = StandardEvaluationContext()

        paramNames.forEachIndexed { i, name ->
            context.setVariable(name, args[i])
        }

        // SpEL → 키 리스트 생성
        val keys = multiLock.keys.flatMap { expression ->
            val parsed = parser.parseExpression(expression).getValue(context)
            when (parsed) {
                is Collection<*> -> parsed.filterIsInstance<String>()
                is String -> listOf(parsed)
                else -> error("Invalid SpEL key: $expression")
            }
        }

        val locks: List<RLock> = keys.map { redissonClient.getLock("lock:$it") }
        val multiLockObj = RedissonMultiLock(*locks.toTypedArray())

        val acquired = multiLockObj.tryLock(
            multiLock.waitTimeMillis,
            multiLock.leaseTimeMillis,
            java.util.concurrent.TimeUnit.MILLISECONDS
        )

        if (!acquired) throw IllegalStateException("MultiLock acquisition failed for keys: $keys")

        return try {
            joinPoint.proceed()
        } finally {
            multiLockObj.unlock()
        }
    }
}