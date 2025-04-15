package io.opusm.opemsedge.infrastructure.protocol.mqtt.command

import io.opusm.opemsedge.core.device.model.Device
import io.opusm.opemsedge.core.device.model.MqttDataPoint
import io.opusm.opemsedge.core.protocol.model.CommandResult
import io.opusm.opemsedge.core.protocol.model.CommandType
import io.opusm.opemsedge.core.protocol.model.DeviceCommand
import io.opusm.opemsedge.infrastructure.protocol.mqtt.MqttDataMapper
import io.opusm.opemsedge.infrastructure.protocol.mqtt.MqttMessageManager
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * 데이터 포인트 쓰기 명령 처리 전략
 */
@Component
class WriteDataPointCommandStrategy(
    private val dataMapper: MqttDataMapper,
    private val connectionManager: MqttConnectionManager
) : MqttCommandStrategy {
    
    private val logger = LoggerFactory.getLogger(WriteDataPointCommandStrategy::class.java)

    override fun execute(device: Device, command: DeviceCommand, messageManager: MqttMessageManager): CommandResult {
        val deviceId = device.id

        if (command.dataPointId == null || command.value == null) {
            return CommandResult(
                deviceId = deviceId,
                commandType = CommandType.WRITE_DATA_POINT,
                success = false,
                error = "데이터 포인트 ID 또는 값이 지정되지 않았습니다"
            )
        }

        val dataPoint = device.dataPoints[command.dataPointId]
        if (dataPoint == null) {
            return CommandResult(
                deviceId = deviceId,
                commandType = CommandType.WRITE_DATA_POINT,
                success = false,
                error = "데이터 포인트를 찾을 수 없습니다: ${command.dataPointId}"
            )
        }

        if (dataPoint !is MqttDataPoint) {
            return CommandResult(
                deviceId = deviceId,
                commandType = CommandType.WRITE_DATA_POINT,
                success = false,
                error = "MQTT 데이터 포인트가 아닙니다: ${command.dataPointId}"
            )
        }

        if (dataPoint.readOnly) {
            return CommandResult(
                deviceId = deviceId,
                commandType = CommandType.WRITE_DATA_POINT,
                success = false,
                error = "읽기 전용 데이터 포인트입니다: ${command.dataPointId}"
            )
        }

        try {
            // MQTT 클라이언트 얻기
            val client = getClient(device)
            if (client == null || !client.isConnected) {
                return CommandResult(
                    deviceId = deviceId,
                    commandType = CommandType.WRITE_DATA_POINT,
                    success = false,
                    error = "MQTT 클라이언트가 연결되어 있지 않습니다"
                )
            }

            // 값을 JSON으로 변환
            val payload = dataMapper.createPayload(dataPoint, command.value)

            // 메시지 생성 및 전송
            val message = MqttMessage(payload.toByteArray(StandardCharsets.UTF_8))
            message.qos = dataPoint.qos

            client.publish(dataPoint.topic, message)

            return CommandResult(
                deviceId = deviceId,
                commandType = CommandType.WRITE_DATA_POINT,
                success = true
            )
        } catch (e: Exception) {
            logger.error("MQTT 데이터 포인트 쓰기 실패: $deviceId, ${dataPoint.id}", e)
            return CommandResult(
                deviceId = deviceId,
                commandType = CommandType.WRITE_DATA_POINT,
                success = false,
                error = "MQTT 데이터 포인트 쓰기 실패: ${e.message}"
            )
        }
    }
    
    private fun getClient(device: Device): MqttClient? {
        return connectionManager.getClient(device.id)
    }
}