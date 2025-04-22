package kr.hhplus.be.server.user

import kr.hhplus.be.server.common.domain.Money
import kr.hhplus.be.server.user.domain.TransactionType
import kr.hhplus.be.server.user.domain.UserPoint
import kr.hhplus.be.server.user.domain.UserPointHistory
import java.time.LocalDateTime

/**
 * 사용자 포인트 관련 테스트 픽스처
 * 테스트 의도에 맞게 사용자 포인트 및 포인트 이력을 생성합니다.
 */
object UserPointTestFixture {
    
    // 기본 사용자 포인트 생성 빌더
    fun userPoint(
        userId: Long = 1L,
        balance: Money = Money.of(1000),
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): UserPointBuilder {
        return UserPointBuilder(
            userId = userId,
            balance = balance,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    // 사용자 포인트 이력 생성 빌더
    fun userPointHistory(
        id: Long? = null,
        userId: Long = 1L,
        type: TransactionType = TransactionType.CHARGE,
        amount: Money = Money.of(1000),
        createdAt: LocalDateTime = LocalDateTime.now()
    ): UserPointHistoryBuilder {
        return UserPointHistoryBuilder(
            id = id,
            userId = userId,
            type = type,
            amount = amount,
            createdAt = createdAt
        )
    }
    
    // 시나리오별 사용자 포인트 생성 메서드
    
    /**
     * 충분한 포인트를 가진 사용자 생성 (1000원)
     */
    fun standardUserPoint(userId: Long = 1L): UserPoint {
        return userPoint(userId = userId, balance = Money.of(1000)).build()
    }
    
    /**
     * 포인트가 없는 사용자 생성
     */
    fun zeroBalanceUserPoint(userId: Long = 1L): UserPoint {
        return userPoint(userId = userId, balance = Money.ZERO).build()
    }
    
    /**
     * 많은 포인트를 가진 사용자 생성 (100,000원)
     */
    fun highBalanceUserPoint(userId: Long = 1L): UserPoint {
        return userPoint(userId = userId, balance = Money.of(100000)).build()
    }
    
    // 시나리오별 사용자 포인트 이력 생성 메서드
    
    /**
     * 포인트 충전 이력 생성
     */
    fun chargePointHistory(
        userId: Long = 1L, 
        amount: Money = Money.of(1000), 
    ): UserPointHistory {
        return userPointHistory(
            userId = userId,
            type = TransactionType.CHARGE,
            amount = amount,
        ).build()
    }
    
    /**
     * 포인트 사용 이력 생성
     */
    fun usePointHistory(
        userId: Long = 1L, 
        amount: Money = Money.of(1000), 
    ): UserPointHistory {
        return userPointHistory(
            userId = userId,
            type = TransactionType.USE,
            amount = amount,
        ).build()
    }

    // 빌더 클래스
    class UserPointBuilder(
        private val userId: Long,
        private val balance: Money,
        private val createdAt: LocalDateTime,
        private val updatedAt: LocalDateTime
    ) {
        fun build(): UserPoint {
            return UserPoint(
                userId = userId,
                balance = balance,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
        
        fun withBalance(balance: Money): UserPointBuilder {
            return UserPointBuilder(
                userId = userId,
                balance = balance,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
        
        fun withUpdatedAt(updatedAt: LocalDateTime): UserPointBuilder {
            return UserPointBuilder(
                userId = userId,
                balance = balance,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }
    
    class UserPointHistoryBuilder(
        private val id: Long?,
        private val userId: Long,
        private val type: TransactionType,
        private val amount: Money,
        private val createdAt: LocalDateTime
    ) {
        fun build(): UserPointHistory {
            return UserPointHistory(
                id = id!!,
                userId = userId,
                amount = amount,
                createdAt = createdAt,
                transactionType = type
            )
        }
        
        fun withAmount(amount: Money): UserPointHistoryBuilder {
            return UserPointHistoryBuilder(
                id = id,
                userId = userId,
                type = type,
                amount = amount,
                createdAt = createdAt
            )
        }
        
        fun withBalance(balance: Money): UserPointHistoryBuilder {
            return UserPointHistoryBuilder(
                id = id,
                userId = userId,
                type = type,
                amount = amount,
                createdAt = createdAt
            )
        }
        
        fun withDescription(description: String): UserPointHistoryBuilder {
            return UserPointHistoryBuilder(
                id = id,
                userId = userId,
                type = type,
                amount = amount,
                createdAt = createdAt
            )
        }
    }
    
    // 기존 호환성 유지 메서드들 (리팩토링 기간 동안만 유지)
    @Deprecated("새로운 userPoint() 빌더를 사용하세요", ReplaceWith("userPoint(userId, balance, updatedAt).build()"))
    fun createUserPoint(
        userId: Long = 1L,
        balance: Money = Money.of(1000),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): UserPoint {
        return userPoint(
            userId = userId,
            balance = balance,
            updatedAt = updatedAt
        ).build()
    }
}