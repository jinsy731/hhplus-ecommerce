package kr.hhplus.be.server.common.infrastructure

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedissonConfig {

    companion object { const val REDIS_URL_PREFIX = "redis://" }

    @Value("\${spring.redis.cluster.nodes}")
    private lateinit var clusterNodes: List<String>

    @Value("\${spring.redis.password:}")
    private var redisPassword: String? = null

    @Bean(destroyMethod = "shutdown")
    fun redissonClient(): RedissonClient {
        val config = Config()
        val clusterServersConfig = config.useClusterServers()

        clusterNodes.forEach {
            clusterServersConfig.addNodeAddress("$REDIS_URL_PREFIX$it")
        }

        if (!redisPassword.isNullOrEmpty()) {
            clusterServersConfig.password = redisPassword
        }

        return Redisson.create(config)
    }
}
