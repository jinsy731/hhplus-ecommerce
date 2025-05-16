package kr.hhplus.be.server

import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class CacheCleaner(private val cacheManager: CacheManager) {
    fun clean(cacheName: String, cacheKey: String) {
        cacheManager.getCache(cacheName)?.evict(cacheKey)
    }
}