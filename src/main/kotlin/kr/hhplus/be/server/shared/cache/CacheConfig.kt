package kr.hhplus.be.server.shared.cache

import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheManager

//@Configuration
class CacheConfig {

//    @Bean
    fun cacheManager(redisCacheManager: RedisCacheManager): CacheManager {
        return redisCacheManager
    }
}