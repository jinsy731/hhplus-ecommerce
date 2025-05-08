package kr.hhplus.be.server.shared.redis

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate

@SpringBootTest
class RedisConfigTestIT {

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `genericRedisTemplate 테스트`() {
        // arrange: 직렬화할 클래스 생성
        val testClass = TestClass(1L, "test")
        val key = "cache:test"
        redisTemplate.opsForValue().set(key, testClass)

        // act: 조회
        val cached = redisTemplate.opsForValue().get(key)
        val converted = objectMapper.convertValue(cached, TestClass::class.java)

        converted.id shouldBe 1L
        converted.name shouldBe "test"
    }

    data class TestClass(
        val id: Long,
        val name: String
    )
}