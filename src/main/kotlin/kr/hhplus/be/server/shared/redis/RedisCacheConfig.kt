package kr.hhplus.be.server.shared.redis

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import java.time.Duration

@Configuration
class RedisCacheConfig(
    private val redisConnectionFactory: RedisConnectionFactory,
    @Qualifier("redisObjectMapper") private val objectMapper: ObjectMapper
) {

    @Bean
    fun redisCacheManager(): CacheManager {
        val serializer = GenericJackson2JsonRedisSerializer(objectMapper)

//        val configMap = mapOf(
//            "productPopular" to RedisCacheConfiguration.defaultCacheConfig()
//                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
//                .entryTtl(Duration.ofMinutes(6)),
//            "productList" to RedisCacheConfiguration.defaultCacheConfig()
//                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
//                .entryTtl(Duration.ofHours(10))
//        )

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .entryTtl(Duration.ofMinutes(10))

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
//            .withInitialCacheConfigurations(configMap)
            .build()
    }
}