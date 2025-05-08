package kr.hhplus.be.server.lock.annotation

import kr.hhplus.be.server.lock.executor.LockType

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithMultiDistributedLock(
    val keys: Array<String> = [],
    val type: LockType = LockType.PUBSUB,
    val waitTimeMillis: Long = 1000,
    val leaseTimeMillis: Long = 3000
)