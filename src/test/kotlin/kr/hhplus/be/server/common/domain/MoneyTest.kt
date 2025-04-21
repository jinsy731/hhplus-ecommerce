package kr.hhplus.be.server.common.domain

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MoneyTest {
    
    @Test
    fun `✅Money 객체 생성`() {
        // arrange
        val money = Money(BigDecimal(100))
        // act, assert
        money.amount shouldBe BigDecimal(100)
    }
    
    @Test
    fun `❌Money 객체 생성 실패_amount가 0보다 작으면 IllegalArgumentException이 발생해야한다`() {
        // arrange
        shouldThrowExactly<IllegalArgumentException> { Money(BigDecimal(-1)) }  
    }
    
    @Test
    fun `✅두 개의 Money 값을 합산`() {
        // arrange
        val money1 = Money(BigDecimal(100))
        val money2 = Money(BigDecimal(200))
        // act
        val moneySum = money1 + money2
        // assert
        moneySum.amount shouldBe BigDecimal(300)
    }

    @Test
    fun `✅두 개의 Money 값을 감산`() {
        // arrange
        val money1 = Money(BigDecimal(300))
        val money2 = Money(BigDecimal(200))
        // act
        val moneySum = money1 - money2
        // assert
        moneySum.amount shouldBe BigDecimal(100)
    }
}