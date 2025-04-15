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
 * 데이터 그룹 읽기 명령 처리 전략
 */
@Component
class ReadDataGroupCommandStrategy(
    private val dataMapper: MqttDataMapper
) : MqttCommandStrategy {
    
    private val logger = LoggerFactory.getLogger(ReadDataGroupCommandStrategy::class.java)

    override fun execute(device: Device, command: DeviceCommand, messageManager: MqttMessageManager): CommandResult {
        val deviceId = device.id

        if (command.dataPointId == null) {
            return CommandResult(
                deviceId = deviceId,
                commandType = CommandType.READ_DATA_GROUP,
                success = false,
                error = "데이터 그룹 ID가 지정되지 않았습니다"
            )
        }

        val dataGroup = device.dataGroups[command.dataPointId]
        if (dataGroup == null) {
            return CommandResult(
                deviceId = deviceId,
                commandType = CommandType.READ_DATA_GROUP,
                success = false,
                error = "데이터 그룹을 찾을 수 없습니다: ${command.dataPointId}"
            )
        }

        val dataPoints = dataGroup.dataPointIds.mapNotNull { device.dataPoints[it] as? MqttDataPoint }

        if (dataPoints.isEmpty()) {
            return CommandResult(
                deviceId = deviceId,
                commandType = CommandType.READ_DATA_GROUP,
                success = false,
                error = "그룹에 MQTT 데이터 포인트가 없습니다: ${dataGroup.id}"
            )
        }
        
        // 데이터 포인트를 토픽별로 그룹화
        val dataPointsByTopic = dataPoints.groupBy { it.topic }
        
        val values = mutableListOf<DataValue>()
        var success = true
        var error: String? = null
        
        try {
            // 각 토픽별로 처리
            for ((topic, topicDataPoints) in dataPointsByTopic) {
                // 해당 토픽의 최신 메시지 가져오기
                val latestMessage = messageManager.getLatestMessage(deviceId, topic)
                
                if (latestMessage == null) {
                    success = false
                    error = "토픽 ${topic}에 대한 수신된 메시지가 없습니다"
                    break
                }
                
                // 토픽에 여러 데이터 포인트가 매핑된 경우 (JSON 전체 처리)
                if (topicDataPoints.size > 1) {
                    logger.debug("토픽 ${topic}에 매핑된 데이터 포인트 ${topicDataPoints.size}개를 일괄 처리합니다")
                    
                    // 여러 데이터 포인트 값을 한 번에 추출
                    val extractedValues = dataMapper.extractMultipleValuesFromMessage(latestMessage, topicDataPoints)
                    values.addAll(extractedValues.values)
                } else {
                    // 토픽에 단일 데이터 포인트만 있는 경우
                    val singleDataPoint = topicDataPoints.first()
                    val value = dataMapper.extractValueFromMessage(latestMessage, singleDataPoint)
                    
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
            }
        } catch (e: Exception) {
            logger.error("데이터 그룹 읽기 실패: $deviceId, ${dataGroup.id}", e)
            success = false
            error = "데이터 그룹 읽기 실패: ${e.message}"
        }

        return CommandResult(
            deviceId = deviceId,
            commandType = CommandType.READ_DATA_GROUP,
            success = success,
            values = values,
            error = error
        )
    }
}