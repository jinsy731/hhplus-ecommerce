package kr.hhplus.be.server.shared

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kr.hhplus.be.server.point.domain.model.TransactionType
import kr.hhplus.be.server.point.domain.model.UserPoint
import kr.hhplus.be.server.point.domain.model.UserPointHistory
import kr.hhplus.be.server.shared.domain.Money
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TestEntityUtilsTest {

    @Test
    fun `엔티티 ID를 성공적으로 설정한다`() {
        // given
        val userPoint = UserPoint(
            userId = 1L,
            balance = Money.of(1000)
        )
        
        // when
        TestEntityUtils.setEntityId(userPoint, 123L)
        
        // then
        userPoint.id shouldBe 123L
    }
    
    @Test
    fun `엔티티 ID가 null에서 값으로 변경된다`() {
        // given
        val userPoint = UserPoint(
            userId = 1L,
            balance = Money.of(1000)
        )
        userPoint.id shouldBe null
        
        // when
        TestEntityUtils.setEntityId(userPoint, 456L)
        
        // then
        userPoint.id shouldBe 456L
    }
    
    @Test
    fun `체이닝을 통해 ID를 설정하고 엔티티를 반환한다`() {
        // given
        val userPoint = UserPoint(
            userId = 1L,
            balance = Money.of(1000)
        )
        
        // when
        val result = TestEntityUtils.setEntityId(userPoint, 789L)
        
        // then
        result shouldBe userPoint
        result.id shouldBe 789L
    }
    
    @Test
    fun `특정 필드 값을 성공적으로 설정한다`() {
        // given
        val userPoint = UserPoint(
            userId = 1L,
            balance = Money.of(1000)
        )
        
        // when
        TestEntityUtils.setEntityField(userPoint, "version", 5L)
        
        // then
        userPoint.version shouldBe 5L
    }
    
    @Test
    fun `특정 필드 값을 성공적으로 조회한다`() {
        // given
        val userPoint = UserPoint(
            userId = 1L,
            balance = Money.of(1000)
        )
        TestEntityUtils.setEntityId(userPoint, 999L)
        
        // when
        val id = TestEntityUtils.getEntityField(userPoint, "id") as Long?
        
        // then
        id shouldBe 999L
    }
    
    @Test
    fun `존재하지 않는 필드에 접근하면 예외가 발생한다`() {
        // given
        val userPoint = UserPoint(
            userId = 1L,
            balance = Money.of(1000)
        )
        
        // when & then
        shouldThrow<RuntimeException> {
            TestEntityUtils.setEntityField(userPoint, "nonExistentField", "value")
        }
    }
    
    @Test
    fun `여러 엔티티의 ID를 순차적으로 설정한다`() {
        // given
        val userPoints = listOf(
            UserPoint(userId = 1L, balance = Money.of(1000)),
            UserPoint(userId = 2L, balance = Money.of(2000)),
            UserPoint(userId = 3L, balance = Money.of(3000))
        )
        
        // when
        TestEntityUtils.setEntityIds(userPoints, startId = 100L)
        
        // then
        userPoints[0].id shouldBe 100L
        userPoints[1].id shouldBe 101L
        userPoints[2].id shouldBe 102L
    }
    
    @Test
    fun `여러 엔티티의 ID를 기본값부터 순차적으로 설정한다`() {
        // given
        val userPointHistories = listOf(
            UserPointHistory(
                userId = 1L,
                transactionType = TransactionType.CHARGE,
                amount = Money.of(1000),
                createdAt = LocalDateTime.now()
            ),
            UserPointHistory(
                userId = 2L,
                transactionType = TransactionType.USE,
                amount = Money.of(500),
                createdAt = LocalDateTime.now()
            )
        )
        
        // when
        TestEntityUtils.setEntityIds(userPointHistories)
        
        // then
        userPointHistories[0].id shouldBe 1L
        userPointHistories[1].id shouldBe 2L
    }
    
    @Test
    fun `다른 타입의 엔티티에도 동일하게 동작한다`() {
        // given
        val userPointHistory = UserPointHistory(
            userId = 1L,
            transactionType = TransactionType.CHARGE,
            amount = Money.of(1000),
            createdAt = LocalDateTime.now()
        )
        
        // when
        TestEntityUtils.setEntityId(userPointHistory, 777L)
        
        // then
        userPointHistory.id shouldBe 777L
    }
    
    @Test
    fun `ID를 null로 다시 설정할 수 있다`() {
        // given
        val userPoint = UserPoint(
            userId = 1L,
            balance = Money.of(1000)
        )
        TestEntityUtils.setEntityId(userPoint, 123L)
        userPoint.id shouldNotBe null
        
        // when
        TestEntityUtils.setEntityField(userPoint, "id", null)
        
        // then
        userPoint.id shouldBe null
    }
} 