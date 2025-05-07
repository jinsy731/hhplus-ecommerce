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
class WithDistributedLockAspect(
    private val lockExecutor: DistributedLockExecutor,
    private val keyResolver: LockKeyResolver
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Around("@annotation(annotation)")
    fun around(joinPoint: ProceedingJoinPoint, annotation: WithDistributedLock): Any? {
        val key = keyResolver.resolveKey(joinPoint, annotation.key).first()
        logger.info("[WithDistributedLock] key resolved: $key")

        return lockExecutor.execute(
            key = key,
            lockType = annotation.type,
            waitTimeMillis = annotation.waitTimeMillis,
            leaseTimeMillis = annotation.leaseTimeMillis
        ) {
            joinPoint.proceed()
        }
    }
}

