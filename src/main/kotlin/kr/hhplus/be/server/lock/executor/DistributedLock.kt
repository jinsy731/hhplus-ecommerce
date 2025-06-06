package kr.hhplus.be.server.lock.executor

interface DistributedLock {
    fun supports(lockType: LockType): Boolean
    fun tryLock(key: String, waitTimeMillis: Long, leaseTimeMillis: Long): Boolean
    fun tryMultiLock(keys: Array<String>, waitTimeMillis: Long, leaseTimeMillis: Long): Boolean
    fun unlock(key: String)
    fun unlockMulti(keys: Array<String>)
}


enum class LockType {
    SPIN,
    PUBSUB
}

