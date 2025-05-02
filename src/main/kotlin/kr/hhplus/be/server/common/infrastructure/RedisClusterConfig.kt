package kr.hhplus.be.server.common.infrastructure

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisClusterConfiguration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
class RedisClusterConfig {

    @Value("\${spring.redis.cluster.nodes}")
    private lateinit var clusterNodes: String

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val nodeList = clusterNodes.split(",").map { it.trim() }
        val clusterConfig = RedisClusterConfiguration(nodeList)

        return LettuceConnectionFactory(clusterConfig)
    }

    @Bean
    fun stringRedisTemplate(connectionFactory: RedisConnectionFactory): StringRedisTemplate {
        return StringRedisTemplate(connectionFactory)
    }
}
