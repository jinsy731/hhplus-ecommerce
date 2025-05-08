package kr.hhplus.be.server.lock.aspect

import kr.hhplus.be.server.lock.annotation.WithMultiDistributedLock
import kr.hhplus.be.server.lock.executor.DistributedLockExecutor
import kr.hhplus.be.server.lock.utils.LockKeyResolver
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Aspect
@Component
@Order(0)
class WithMultiLockAspect(
    private val lockExecutor: DistributedLockExecutor,
    private val keyResolver: LockKeyResolver
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Around("@annotation(annotation)")
    fun around(joinPoint: ProceedingJoinPoint, annotation: WithMultiDistributedLock): Any? {
        val keys = keyResolver.resolveKey(joinPoint, *annotation.keys)
        logger.info("[WithMultiLock] keys resolved: $keys")

        return lockExecutor.executeMulti(
            keys = keys.toTypedArray(),
            lockType = annotation.type,
            waitTimeMillis = annotation.waitTimeMillis,
            leaseTimeMillis = annotation.leaseTimeMillis
        ) { joinPoint.proceed() }
    }
}
