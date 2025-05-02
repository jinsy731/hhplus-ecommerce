package kr.hhplus.be.server.common.infrastructure

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedissonClusterConfig {

    @Value("\${spring.redis.cluster.nodes}")
    private lateinit var clusterNodes: String

    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config()

        val nodeList = clusterNodes.split(",")
            .map { "redis://$it" }

        config.useClusterServers()
            .addNodeAddress(*nodeList.toTypedArray())
            .setScanInterval(2000)
            .setRetryAttempts(3)
            .setRetryInterval(1500)

        return Redisson.create(config)
    }
}
