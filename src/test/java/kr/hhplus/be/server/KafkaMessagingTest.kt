package kr.hhplus.be.server

import kr.hhplus.be.server.shared.domain.DomainEvent
import kr.hhplus.be.server.shared.event.KafkaEventMessage
import kr.hhplus.be.server.shared.event.MessageProducer
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest
class KafkaMessagingTest {
    @Autowired
    private lateinit var kafkaPublisher: MessageProducer

    @Test
    fun `메시지 발행 테스트`() {
        repeat(10) {
            kafkaPublisher.publish(TestEvent("test-${it})"))
        }

        KafkaEventListener.latch.await(10, TimeUnit.SECONDS)
    }

    @Test
    fun `메시지 배치 수신 테스트`() {
        executeConcurrently(100) {
            kafkaPublisher.publish(TestBatchEvent("test-batch-${it}"))
        }

        KafkaEventBatchListener.latch.await(10, TimeUnit.SECONDS)
    }

    @Test
    fun `메시지 처리 실패 테스트__메시지 처리에 실패하면 같은 메시지를 다시 수신한다`() {
        kafkaPublisher.publish(TestFailEvent("failed-message-1"))
        KafkaFailEventListener.latch.await(10, TimeUnit.SECONDS)
    }
}
@Component
class KafkaEventListener() {
    private val logger = LoggerFactory.getLogger(KafkaEventListener::class.java)

    companion object { val latch = CountDownLatch(10) }

    @KafkaListener(topics = ["test.v1"], groupId = "test-group")
    fun onReceived(
        @Payload message: KafkaEventMessage,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        logger.info("Received message: $message, topic: $topic, partition: $partition, offset: $offset")
        latch.countDown()
        acknowledgment.acknowledge()
    }
}

@Component
class KafkaEventBatchListener() {
    private val logger = LoggerFactory.getLogger(KafkaEventListener::class.java)

    companion object { val latch = CountDownLatch(1) }
    private var processedMessageCount = 0

    @KafkaListener(topics = ["test-batch.v1"], groupId = "test-group", batch = "true")
    fun onReceivedBatch(
        @Payload message: List<KafkaEventMessage>,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        logger.info("Received message size: ${message.size}")
        logger.info("Received message: $message, topic: $topic, partition: $partition, offset: $offset")
        processedMessageCount += message.size
        if(processedMessageCount == 100) latch.countDown()
        acknowledgment.acknowledge()
    }
}

@Component
class KafkaFailEventListener() {
    private val logger = LoggerFactory.getLogger(KafkaEventListener::class.java)

    companion object { val latch = CountDownLatch(1) }
    private var lastOffset = -1L

    @KafkaListener(topics = ["test-fail.v1"], groupId = "test-group")
    fun onReceivedBatch(
        @Payload message: KafkaEventMessage,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        logger.info("Received message: $message, topic: $topic, partition: $partition, offset: $offset")
        logger.info("lastOffset == offset: ${lastOffset == offset}")
        if(lastOffset == offset) {
            latch.countDown()
        }
        lastOffset = offset
        throw RuntimeException()
    }
}

data class TestEvent(override val payload: Any): DomainEvent<Any>() {
    override val eventType: String
        get() = "test"
}

data class TestFailEvent(override val payload: Any): DomainEvent<Any>() {
    override val eventType: String
        get() = "test-fail"
}

data class TestBatchEvent(override val payload: Any): DomainEvent<Any>() {
    override val eventType: String
        get() = "test-batch"
}