package kr.hhplus.be.server.order.infrastructure

import kr.hhplus.be.server.order.application.OrderResultSender
import kr.hhplus.be.server.order.domain.model.Order
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class FakeOrderResultSender : OrderResultSender {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun send(order: Order) {
        logger.info("Order Message Published..")
    }
}