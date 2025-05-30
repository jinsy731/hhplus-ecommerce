package kr.hhplus.be.server.shared.redis

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.product.application.dto.ProductResult
import kr.hhplus.be.server.product.infrastructure.ProductListDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Value("\${spring.data.redis.host}")
    private lateinit var redisHost: String

    @Value("\${spring.data.redis.port}")
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

    @Bean
    fun genericRedisTemplate(connectionFactory: RedisConnectionFactory, objectMapper: ObjectMapper): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory

        val serializer = GenericJackson2JsonRedisSerializer(objectMapper)
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = serializer
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = serializer

        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun productRedisTemplate(connectionFactory: RedisConnectionFactory, objectMapper: ObjectMapper): RedisTemplate<String, ProductListDto> {
        val template = RedisTemplate<String, ProductListDto>()
        template.connectionFactory = connectionFactory

        val serializer = Jackson2JsonRedisSerializer(objectMapper, ProductListDto::class.java)

        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = serializer
        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun productResultRedisTemplate(connectionFactory: RedisConnectionFactory, objectMapper: ObjectMapper): RedisTemplate<String, ProductResult.RetrieveList> {
        val template = RedisTemplate<String, ProductResult.RetrieveList>()
        template.connectionFactory = connectionFactory

        val serializer = Jackson2JsonRedisSerializer(objectMapper, ProductResult.RetrieveList::class.java)

        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = serializer
        template.afterPropertiesSet()
        return template
    }
}
