package kr.hhplus.be.server.order.infrastructure

import kr.hhplus.be.server.order.domain.OrderResultSender
import kr.hhplus.be.server.order.domain.event.OrderCompletedPayload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FakeOrderResultSender : OrderResultSender {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun send(order: OrderCompletedPayload) {
        logger.info("Order Message Published..")
    }
}