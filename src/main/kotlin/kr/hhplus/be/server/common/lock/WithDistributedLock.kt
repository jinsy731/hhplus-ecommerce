package kr.hhplus.be.server.common.lock

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithDistributedLock(
    val key: String,
    val type: LockType = LockType.PUBSUB,
    val waitTimeMillis: Long = 1000,
    val leaseTimeMillis: Long = 3000
)
