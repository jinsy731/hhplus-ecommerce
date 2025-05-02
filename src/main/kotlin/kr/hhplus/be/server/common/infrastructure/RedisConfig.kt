package kr.hhplus.be.server.common.infrastructure

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
class RedisConfig {

    @Value("\${spring.redis.host}")
    private lateinit var redisHost: String

    @Value("\${spring.redis.port}")
    private var redisPort: Int = 6379

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val standaloneConfig = RedisStandaloneConfiguration(redisHost, redisPort)
        return LettuceConnectionFactory(standaloneConfig)
    }

    @Bean
    fun stringRedisTemplate(factory: RedisConnectionFactory): StringRedisTemplate {
        return StringRedisTemplate(factory)
    }
}
