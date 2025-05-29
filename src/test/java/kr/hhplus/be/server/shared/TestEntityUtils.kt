package kr.hhplus.be.server.shared

import kotlin.reflect.full.memberProperties

/**
 * 테스트에서 엔티티의 ID를 설정하기 위한 리플렉션 유틸리티
 * 
 * JPA 엔티티의 ID 필드는 보통 val로 선언되고 auto increment를 위해 null로 초기화됩니다.
 * 단위 테스트에서는 특정 ID 값을 가진 엔티티가 필요한 경우가 있는데,
 * 이 유틸리티를 사용하면 리플렉션을 통해 ID 값을 강제로 설정할 수 있습니다.
 */
object TestEntityUtils {
    
    /**
     * 엔티티 객체의 ID 필드에 값을 설정합니다.
     * 
     * @param entity ID를 설정할 엔티티 객체
     * @param id 설정할 ID 값
     * @return ID가 설정된 엔티티 객체 (체이닝을 위해)
     * 
     * @throws NoSuchFieldException ID 필드를 찾을 수 없을 때
     * @throws IllegalAccessException 필드에 접근할 수 없을 때
     * 
     * 사용 예시:
     * ```kotlin
     * val userPoint = UserPoint(userId = 1L, balance = Money.of(1000))
     * TestEntityUtils.setEntityId(userPoint, 123L)
     * // userPoint.id는 이제 123L이 됩니다.
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> setEntityId(entity: T, id: Long): T {
        return setEntityField(entity, "id", id)
    }
    
    /**
     * 엔티티 객체의 특정 필드에 값을 설정합니다.
     * 
     * @param entity 필드 값을 설정할 엔티티 객체
     * @param fieldName 설정할 필드명
     * @param value 설정할 값
     * @return 필드가 설정된 엔티티 객체 (체이닝을 위해)
     * 
     * @throws NoSuchFieldException 필드를 찾을 수 없을 때
     * @throws IllegalAccessException 필드에 접근할 수 없을 때
     * 
     * 사용 예시:
     * ```kotlin
     * val userPoint = UserPoint(userId = 1L, balance = Money.of(1000))
     * TestEntityUtils.setEntityField(userPoint, "version", 5L)
     * // userPoint.version은 이제 5L이 됩니다.
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> setEntityField(entity: T, fieldName: String, value: Any?): T {
        try {
            // Kotlin 리플렉션을 사용하여 필드 찾기
            val kClass = entity::class
            val property = kClass.memberProperties.find { it.name == fieldName }
                ?: throw NoSuchFieldException("Field '$fieldName' not found in ${kClass.simpleName}")
            
            // Java 리플렉션으로 변환하여 값 설정
            val javaField = entity::class.java.getDeclaredField(fieldName)
            javaField.isAccessible = true
            javaField.set(entity, value)
            
            return entity
        } catch (e: Exception) {
            throw RuntimeException("Failed to set field '$fieldName' on entity ${entity::class.simpleName}", e)
        }
    }
    
    /**
     * 엔티티 객체의 특정 필드 값을 조회합니다.
     * 
     * @param entity 필드 값을 조회할 엔티티 객체
     * @param fieldName 조회할 필드명
     * @return 필드 값
     * 
     * @throws NoSuchFieldException 필드를 찾을 수 없을 때
     * @throws IllegalAccessException 필드에 접근할 수 없을 때
     * 
     * 사용 예시:
     * ```kotlin
     * val userPoint = UserPoint(userId = 1L, balance = Money.of(1000))
     * val id = TestEntityUtils.getEntityField(userPoint, "id") as Long?
     * ```
     */
    fun <T : Any> getEntityField(entity: T, fieldName: String): Any? {
        try {
            val javaField = entity::class.java.getDeclaredField(fieldName)
            javaField.isAccessible = true
            return javaField.get(entity)
        } catch (e: Exception) {
            throw RuntimeException("Failed to get field '$fieldName' from entity ${entity::class.simpleName}", e)
        }
    }
    
    /**
     * 여러 엔티티 객체들의 ID를 순차적으로 설정합니다.
     * 
     * @param entities ID를 설정할 엔티티 객체들
     * @param startId 시작 ID (기본값: 1)
     * @return ID가 설정된 엔티티 객체들
     * 
     * 사용 예시:
     * ```kotlin
     * val userPoints = listOf(
     *     UserPoint(userId = 1L, balance = Money.of(1000)),
     *     UserPoint(userId = 2L, balance = Money.of(2000)),
     *     UserPoint(userId = 3L, balance = Money.of(3000))
     * )
     * TestEntityUtils.setEntityIds(userPoints, startId = 100L)
     * // userPoints[0].id = 100L, userPoints[1].id = 101L, userPoints[2].id = 102L
     * ```
     */
    fun <T : Any> setEntityIds(entities: List<T>, startId: Long = 1L): List<T> {
        entities.forEachIndexed { index, entity ->
            setEntityId(entity, startId + index)
        }
        return entities
    }
} 