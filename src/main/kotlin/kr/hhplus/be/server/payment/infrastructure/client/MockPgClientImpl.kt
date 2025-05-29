package kr.hhplus.be.server.payment.infrastructure.client

import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * PG 클라이언트의 모킹 구현체
 * 실제로는 외부 PG API를 호출하지만, 여기서는 성공 응답을 시뮬레이션
 */
@Component
class MockPgClientImpl : PgClient {
    
    override fun processPayment(request: PgPaymentRequest): PgPaymentResult {
        // 실제로는 외부 PG API 호출
        // 여기서는 성공했다고 가정하여 시뮬레이션
        
        return PgPaymentResult(
            pgPaymentId = request.pgPaymentId,
            status = PgPaymentStatus.SUCCESS,
            paidAmount = request.amount,
            paidAt = LocalDateTime.now(),
            message = "결제가 성공적으로 완료되었습니다."
        )
    }
} 