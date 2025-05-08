package kr.hhplus.be.server

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisCleaner {
    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    fun clean() {
        val keys = redisTemplate.keys("*")
        if (!keys.isNullOrEmpty()) {
            redisTemplate.delete(keys)
        }
    }
}