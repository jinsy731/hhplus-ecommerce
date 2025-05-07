package kr.hhplus.be.server.common.lock

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
        val keys = keyResolver.resolveKey(joinPoint, annotation.key)
        logger.info("[WithMultiLock] keys resolved: $keys")

        return lockExecutor.executeMulti(
            keys = keys.toTypedArray(),
            lockType = annotation.type,
            waitTimeMillis = annotation.waitTimeMillis,
            leaseTimeMillis = annotation.leaseTimeMillis
        ) { joinPoint.proceed() }
    }
}
