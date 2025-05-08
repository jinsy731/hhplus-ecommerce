package kr.hhplus.be.server.shared.redis

import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import java.time.Duration

@Configuration
class RedisCacheConfig(
    private val redisConnectionFactory: RedisConnectionFactory
) {

    @Bean
    fun redisCacheManager(): CacheManager {
        val configMap = mapOf(
            "productPopular" to RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(6)),
            "productList" to RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(10))
        )

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
            .withInitialCacheConfigurations(configMap)
            .build()
    }
}