package kr.hhplus.be.server.common

import kr.hhplus.be.server.order.domain.Order
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FakeMessagingService : MessagingService {
    private val logger = LoggerFactory.getLogger(javaClass)
    override fun publish(order: Order) {
        logger.info("Order Message Published..")
    }
}