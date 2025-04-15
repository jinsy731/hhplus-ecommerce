package io.opusm.opemsedge.infrastructure.protocol.mqtt.command

import io.opusm.opemsedge.core.protocol.model.CommandType
import org.springframework.stereotype.Component
/**
 * MQTT 명령 처리 전략 팩토리
 * 명령 유형에 따라 적절한 전략 인스턴스를 제공합니다.
 */
@Component
class MqttCommandStrategyFactory(
    readDataPointCommandStrategy: ReadDataPointCommandStrategy,
    writeDataPointCommandStrategy: WriteDataPointCommandStrategy,
    readDataGroupCommandStrategy: ReadDataGroupCommandStrategy,
    scanAllCommandStrategy: ScanAllCommandStrategy
) {
    private val strategies = mapOf(
        CommandType.READ_DATA_POINT to readDataPointCommandStrategy,
        CommandType.WRITE_DATA_POINT to writeDataPointCommandStrategy,
        CommandType.READ_DATA_GROUP to readDataGroupCommandStrategy,
        CommandType.SCAN_ALL to scanAllCommandStrategy
    )
    
    /**
     * 명령 유형에 맞는 전략 인스턴스 제공
     */
    fun getStrategy(commandType: CommandType): MqttCommandStrategy? {
        return strategies[commandType]
    }
    
    /**
     * 지원하는 명령 유형 목록 제공
     */
    fun getSupportedCommandTypes(): Set<CommandType> {
        return strategies.keys
    }
}