package kr.hhplus.be.server.shared.event

import kr.hhplus.be.server.order.domain.OrderEvent
import kr.hhplus.be.server.order.domain.OrderEventPayload
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
class SpringDomainEventPublisherTestIT @Autowired constructor(private val springDomainEventPublisher: SpringDomainEventPublisher){
    
    @Test
    fun `이벤트 발행`() {
        springDomainEventPublisher.publish(OrderEvent.Completed(
            payload = OrderEventPayload.Order(
                orderId = 1L ,
                items = listOf(OrderEventPayload.OrderItem(1L, 1L, BigDecimal.TEN)),
                totalAmount = BigDecimal.TEN
            )))
    }
}