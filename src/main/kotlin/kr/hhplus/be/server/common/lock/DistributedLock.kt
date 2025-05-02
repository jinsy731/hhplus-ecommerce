package kr.hhplus.be.server.common.lock

interface DistributedLock {
    fun supports(lockType: LockType): Boolean
    fun tryLock(key: String, waitTimeMillis: Long, leaseTimeMillis: Long): Boolean
    fun unlock(key: String)
}


enum class LockType {
    SPIN,
    PUBSUB
}

