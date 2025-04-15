package io.opusm.opemsedge.infrastructure.protocol.mqtt.command

import io.opusm.opemsedge.core.device.model.DataQuality
import io.opusm.opemsedge.core.device.model.DataValue
import io.opusm.opemsedge.core.device.model.Device
import io.opusm.opemsedge.core.device.model.MqttDataPoint
import io.opusm.opemsedge.core.protocol.model.CommandResult
import io.opusm.opemsedge.core.protocol.model.CommandType
import io.opusm.opemsedge.core.protocol.model.DeviceCommand
import io.opusm.opemsedge.infrastructure.protocol.mqtt.MqttDataMapper
import io.opusm.opemsedge.infrastructure.protocol.mqtt.MqttMessageManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * 전체 데이터 스캔 명령 처리 전략
 */
@Component
class ScanAllCommandStrategy(
    private val dataMapper: MqttDataMapper
) : MqttCommandStrategy {
    
    private val logger = LoggerFactory.getLogger(ScanAllCommandStrategy::class.java)

    override fun execute(device: Device, command: DeviceCommand, messageManager: MqttMessageManager): CommandResult {
        val deviceId = device.id

        // MQTT 데이터 포인트만 필터링
        val dataPoints = device.dataPoints.values.filterIsInstance<MqttDataPoint>()

        if (dataPoints.isEmpty()) {
            return CommandResult(
                deviceId = deviceId,
                commandType = CommandType.SCAN_ALL,
                success = false,
                error = "장치에 MQTT 데이터 포인트가 없습니다"
            )
        }

        // 토픽별로 데이터 포인트 그룹화
        val dataPointsByTopic = dataPoints.groupBy { it.topic }
        
        val values = mutableListOf<DataValue>()
        var failureCount = 0
        var lastError: String? = null
        
        // 각 토픽별로 데이터 포인트 처리
        for ((topic, topicDataPoints) in dataPointsByTopic) {
            try {
                // 토픽의 최신 메시지 가져오기
                val message = messageManager.getLatestMessage(deviceId, topic)
                
                if (message == null) {
                    failureCount++
                    lastError = "토픽 ${topic}에 대한 수신된 메시지가 없습니다"
                    continue
                }
                
                // 토픽에 여러 데이터 포인트가 매핑된 경우 (JSON 전체 처리)
                if (topicDataPoints.size > 1) {
                    logger.debug("토픽 ${topic}에 매핑된 데이터 포인트 ${topicDataPoints.size}개를 일괄 처리합니다")
                    
                    // JSON에서 여러 필드 값을 한 번에 추출
                    val extractedValues = dataMapper.extractMultipleValuesFromMessage(message, topicDataPoints)
                    values.addAll(extractedValues.values)
                } else {
                    // 토픽에 단일 데이터 포인트만 있는 경우
                    val singleDataPoint = topicDataPoints.first()
                    val value = dataMapper.extractValueFromMessage(message, singleDataPoint)
                    
                    values.add(
                        DataValue(
                            dataPointId = singleDataPoint.id,
                            rawValue = value,
                            scaledValue = value,
                            quality = DataQuality.GOOD,
                            timestamp = Instant.now()
                        )
                    )
                }
            } catch (e: Exception) {
                failureCount++
                lastError = "토픽 $topic 처리 중 오류: ${e.message}"
                logger.error("MQTT 데이터 포인트 스캔 중 오류: $topic", e)
            }
        }

        val success = failureCount == 0
        val errorMessage = if (success) null else "$failureCount 개의 토픽 처리 실패. 마지막 오류: $lastError"

        return CommandResult(
            deviceId = deviceId,
            commandType = CommandType.SCAN_ALL,
            success = success,
            values = values,
            error = errorMessage
        )
    }
}