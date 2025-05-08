package kr.hhplus.be.server.shared.messaging

import kr.hhplus.be.server.order.domain.Order
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class FakeMessagingService : MessagingService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun publish(order: Order) {
        logger.info("Order Message Published..")
    }
}